#!/bin/sh
# Tests for pair.sh. Commands the script calls are replaced by shell functions.

here=$(cd "$(dirname "$0")" && pwd)
. "$here/../tests/assert.sh"
DEBOUT_PAIR_SOURCED=1 . "$here/pair.sh"

# --- Language

check 'French locale' fr "$(debout_language fr_FR.UTF-8)"
check 'French Apple language' fr "$(debout_language fr-CA)"
check 'bare French' fr "$(debout_language fr)"
check 'English locale' en "$(debout_language en-US)"
check 'other language' en "$(debout_language de_DE.UTF-8)"
check 'no locale' en "$(debout_language '')"
check 'not French despite the prefix' en "$(debout_language fry)"

defaults() {
    printf '(\n    "fr-FR",\n    en\n)\n'
}
check 'first Apple language' fr-FR "$(system_locale)"

defaults() { return 1; }
check 'falls back to LANG' de_DE.UTF-8 "$(LC_ALL='' LC_MESSAGES='' LANG=de_DE.UTF-8 system_locale)"

# --- Interfaces

HARDWARE_PORTS='Hardware Port: Ethernet
Device: en0
Ethernet Address: aa:bb:cc:dd:ee:ff

Hardware Port: Wi-Fi
Device: en1
Ethernet Address: 11:22:33:44:55:66

Hardware Port: Thunderbolt Bridge
Device: bridge0
Ethernet Address: 36:f9:e3:95:d4:40

Hardware Port: Bluetooth PAN
Device: en5
Ethernet Address: N/A
'

check 'parses hardware ports' 'en0|Ethernet|aa:bb:cc:dd:ee:ff
en1|Wi-Fi|11:22:33:44:55:66
bridge0|Thunderbolt Bridge|36:f9:e3:95:d4:40
en5|Bluetooth PAN|N/A' "$(echo "$HARDWARE_PORTS" | parse_hardware_ports)"

check 'prefers Ethernet over Wi-Fi' 'en0|Ethernet|aa|192.168.1.42' "$(printf '%s\n' \
    'en1|Wi-Fi|11|192.168.1.43' 'en0|Ethernet|aa|192.168.1.42' | select_interface)"

check 'falls back to Wi-Fi' 'en1|Wi-Fi|11|192.168.1.43' "$(printf '%s\n' \
    'en0|Ethernet|aa|' 'en1|Wi-Fi|11|192.168.1.43' | select_interface)"

check 'skips self-assigned addresses' 'en1|Wi-Fi|11|192.168.1.43' "$(printf '%s\n' \
    'en0|Ethernet|aa|169.254.3.4' 'en1|Wi-Fi|11|192.168.1.43' | select_interface)"

check 'skips tethering and bridges' 'en1|Wi-Fi|11|192.168.1.43' "$(printf '%s\n' \
    'en7|iPhone USB|22|172.20.10.2' 'bridge0|Thunderbolt Bridge|33|10.0.0.1' 'en1|Wi-Fi|11|192.168.1.43' | select_interface)"

check 'finds nothing without an address' '' "$(printf '%s\n' 'en0|Ethernet|aa|' | select_interface)"

# --- Wake for network access

pmset() { printf 'System-wide power settings:\nCurrently in use:\n standby              1\n womp                 0\n sleep                1\n'; }
check 'reads womp off' 0 "$(wake_on_magic_packet)"

pmset() { printf 'Currently in use:\n sleep                1\n'; }
check 'no womp setting' '' "$(wake_on_magic_packet)"

# --- Pairing URL

check 'keeps unreserved characters' 'Mac-mini_1.local~' "$(url_encode 'Mac-mini_1.local~')"
check 'encodes spaces, colons and accents' 'Mac%20d%E2%80%99%C3%89lise%3A' "$(url_encode 'Mac d’Élise:')"
check 'uppercases the MAC' 'AA:BB:CC:DD:EE:FF' "$(format_mac aa:bb:cc:dd:ee:ff)"
check 'builds the pairing URL' \
    'https://debout.sybers.fr/pair/#v1?mac=AA%3ABB%3ACC%3ADD%3AEE%3AFF&ip=192.168.1.42&name=Mac%20mini&os=macos&host=Mac-mini.local' \
    "$(pairing_url https://debout.sybers.fr/ AA:BB:CC:DD:EE:FF 192.168.1.42 'Mac mini' Mac-mini.local)"

# --- Confirmation

for answer in y Yes o ' OUI '; do
    is_yes "$answer" && result=yes || result=no
    check "accepts '$answer'" yes "$result"
done
for answer in '' n non yolo; do
    is_yes "$answer" && result=yes || result=no
    check "refuses '$answer'" no "$result"
done

# --- Whole run

LOG=$(mktemp)
trap 'rm -f "$LOG"' EXIT

defaults() { printf '(\n    "en-US"\n)\n'; }
networksetup() { echo "$HARDWARE_PORTS"; }
ipconfig() {
    case $2 in
        en0) echo 192.168.1.42 ;;
        en1) echo 192.168.1.43 ;;
        *) return 1 ;;
    esac
}
scutil() {
    case $2 in
        ComputerName) echo 'Mac mini' ;;
        LocalHostName) echo Mac-mini ;;
    esac
}
pmset() { printf ' womp                 0\n'; }
sudo() { echo "sudo $*" >>"$LOG"; }
open() { echo "open $*" >>"$LOG"; }

ask() { echo y; }
output=$(main 2>&1)
check_contains 'shows the wired interface' 'Ethernet (en0)' "$output"
check_contains 'reports womp off' 'is off' "$output"
check 'turns womp on, then opens the page' 'sudo pmset -a womp 1
open https://debout.sybers.fr/pair/#v1?mac=AA%3ABB%3ACC%3ADD%3AEE%3AFF&ip=192.168.1.42&name=Mac%20mini&os=macos&host=Mac-mini.local' "$(cat "$LOG")"

: >"$LOG"
ask() { echo n; }
main >/dev/null 2>&1
check 'changes nothing without consent' 'open https://debout.sybers.fr/pair/#v1?mac=AA%3ABB%3ACC%3ADD%3AEE%3AFF&ip=192.168.1.42&name=Mac%20mini&os=macos&host=Mac-mini.local' "$(cat "$LOG")"

: >"$LOG"
pmset() { printf ' womp                 1\n'; }
ask() { echo FAIL-asked >>"$LOG"; }
main >/dev/null 2>&1
check 'asks nothing when womp is on' 1 "$(grep -c '^open ' "$LOG")"
check 'never asks when womp is on' 0 "$(grep -c 'FAIL-asked' "$LOG")"

ipconfig() { return 1; }
(main >/dev/null 2>&1)
check 'fails without a connected interface' 1 "$?"

finish macos/pair.sh
