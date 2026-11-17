#!/bin/sh
# Tests for pair.sh. Commands the script calls are replaced by shell functions, and
# /sys/class/net by a temporary directory.

here=$(cd "$(dirname "$0")" && pwd)
. "$here/../tests/assert.sh"
DEBOUT_PAIR_SOURCED=1 . "$here/pair.sh"

DEBOUT_SYS_NET=$(mktemp -d)
LOG=$(mktemp)
trap 'rm -rf "$DEBOUT_SYS_NET" "$LOG"' EXIT

# fake_interface NAME MAC [wireless]
fake_interface() {
    mkdir -p "$DEBOUT_SYS_NET/$1/device"
    echo "$2" >"$DEBOUT_SYS_NET/$1/address"
    [ "${3:-}" != wireless ] || mkdir -p "$DEBOUT_SYS_NET/$1/wireless"
}
fake_interface enp3s0 aa:bb:cc:dd:ee:ff
fake_interface wlp2s0 11:22:33:44:55:66 wireless
fake_interface enp4s0 00:00:00:00:00:01
mkdir -p "$DEBOUT_SYS_NET/docker0"

# --- Language

check 'French locale' fr "$(debout_language fr_FR.UTF-8)"
check 'English locale' en "$(debout_language en_US.UTF-8)"
check 'C locale' en "$(debout_language C.UTF-8)"
check 'no locale' en "$(debout_language '')"

# --- Interfaces

ADDRESSES='2: enp3s0    inet 192.168.1.42/24 brd 192.168.1.255 scope global dynamic enp3s0\       valid_lft 85000sec preferred_lft 85000sec
3: wlp2s0    inet 192.168.1.43/24 brd 192.168.1.255 scope global dynamic wlp2s0\       valid_lft 85000sec preferred_lft 85000sec
4: docker0    inet 172.17.0.1/16 brd 172.17.255.255 scope global docker0\       valid_lft forever preferred_lft forever'
ROUTES='default via 192.168.1.1 dev enp3s0 proto dhcp metric 100
default via 192.168.1.1 dev wlp2s0 proto dhcp metric 600'

check 'parses addresses' 'enp3s0|192.168.1.42
wlp2s0|192.168.1.43
docker0|172.17.0.1' "$(echo "$ADDRESSES" | parse_addresses)"
check 'parses default routes' 'enp3s0
wlp2s0' "$(echo "$ROUTES" | parse_default_interfaces)"

check 'prefers Ethernet over Wi-Fi' 'enp3s0|192.168.1.42' \
    "$(printf '%s\n' 'wlp2s0|192.168.1.43' 'enp3s0|192.168.1.42' | select_interface 'enp3s0 wlp2s0')"
check 'prefers reaching the router over a direct link' 'wlp2s0|192.168.1.43' \
    "$(printf '%s\n' 'enp4s0|10.0.0.1' 'wlp2s0|192.168.1.43' | select_interface 'wlp2s0')"
check 'skips virtual interfaces' 'wlp2s0|192.168.1.43' \
    "$(printf '%s\n' 'docker0|172.17.0.1' 'wlp2s0|192.168.1.43' | select_interface 'docker0 wlp2s0')"
check 'finds nothing without a physical interface' '' "$(printf '%s\n' 'docker0|172.17.0.1' | select_interface docker0)"

# --- Wake-on-LAN

ethtool_output() {
    printf 'Settings for enp3s0:\n\tSupported ports: [ TP ]\n\tSupports Wake-on: %s\n\tWake-on: %s\n\tLink detected: yes\n' "$1" "$2"
}
check 'magic packet on' on "$(ethtool_output pumbg g | parse_wake_on)"
check 'magic packet off' off "$(ethtool_output pumbg d | parse_wake_on)"
check 'magic packet among other wake sources' on "$(ethtool_output pumbg ug | parse_wake_on)"
check 'no magic packet support' unsupported "$(ethtool_output d d | parse_wake_on)"
check 'settings hidden without root' '' "$(printf 'Settings for enp3s0:\n\tLink detected: yes\n' | parse_wake_on)"

have() { [ "$1" = sudo ]; }
ethtool() { printf 'Settings for enp3s0:\n\tLink detected: yes\n'; }
sudo() {
    [ "$1" = -n ] && shift
    [ "$1" = ethtool ] && ethtool_output pumbg d
}
check 'reads the settings through passwordless sudo' off "$(wake_on_status enp3s0)"

have() { return 1; }
check 'no NetworkManager' '' "$(networkmanager_connection enp3s0)"

# --- Pairing payload

check 'builds the payload' 'v1?mac=AA%3ABB%3ACC%3ADD%3AEE%3AFF&ip=192.168.1.42&name=homelab&os=linux' \
    "$(pairing_query AA:BB:CC:DD:EE:FF 192.168.1.42 homelab '')"
check 'adds the mDNS name' 'v1?mac=AA%3ABB%3ACC%3ADD%3AEE%3AFF&ip=192.168.1.42&name=homelab&os=linux&host=homelab.local' \
    "$(pairing_query AA:BB:CC:DD:EE:FF 192.168.1.42 homelab homelab.local)"

# --- Whole run

PAYLOAD='v1?mac=AA%3ABB%3ACC%3ADD%3AEE%3AFF&ip=192.168.1.42&name=homelab&os=linux'
ip() {
    case $* in
        '-o -4 addr show scope global') echo "$ADDRESSES" ;;
        'route show default') echo "$ROUTES" ;;
    esac
}
hostname() { echo homelab; }
ethtool() { ethtool_output pumbg d; }
nmcli() { echo 'Wired connection 1'; }
sudo() { echo "sudo $*" >>"$LOG"; }
qrencode() { echo "qrencode $*" >>"$LOG"; }

HAVE='ethtool nmcli sudo qrencode'
have() { case " $HAVE " in *" $1 "*) return 0 ;; *) return 1 ;; esac; }

ask() { echo o; }
output=$(LC_ALL='' LC_MESSAGES='' LANG=fr_FR.UTF-8 main 2>&1)
check_contains 'speaks French' 'Le Wake-on-LAN par magic packet est désactivé sur enp3s0.' "$output"
check_contains 'names the NetworkManager connection' '« Wired connection 1 »' "$output"
check 'fixes now and permanently, then shows the QR code in the terminal' "sudo ethtool -s enp3s0 wol g
sudo nmcli connection modify Wired connection 1 802-3-ethernet.wake-on-lan magic
qrencode -t ansiutf8 debout://$PAYLOAD" "$(cat "$LOG")"

: >"$LOG"
HAVE='ethtool sudo qrencode'
output=$(main 2>&1)
check_contains 'warns that ethtool alone may not last' 'may not survive a reboot' "$output"
check 'fixes with ethtool alone' "sudo ethtool -s enp3s0 wol g
qrencode -t ansiutf8 debout://$PAYLOAD" "$(cat "$LOG")"

: >"$LOG"
ask() { echo n; }
main >/dev/null 2>&1
check 'changes nothing without consent' "qrencode -t ansiutf8 debout://$PAYLOAD" "$(cat "$LOG")"

: >"$LOG"
HAVE='sudo'
output=$(main 2>&1)
check_contains 'asks for ethtool when it is missing' 'ethtool is not installed' "$output"
check_contains 'prints the URL on a headless machine' "https://debout.sybers.fr/pair/#$PAYLOAD" "$output"
check 'touches nothing without ethtool' '' "$(cat "$LOG")"

ip() { :; }
(main >/dev/null 2>&1)
check 'fails without a connected interface' 1 "$?"

finish linux/pair.sh
