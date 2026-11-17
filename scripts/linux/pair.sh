#!/bin/sh
# Debout: pair this Linux machine with the Debout Android app.
#
# Finds the active network interface (Ethernet first), checks Wake-on-LAN with
# ethtool and offers to turn it on, then shows a QR code to scan with Debout:
# in the terminal when qrencode is installed, otherwise on a web page. The
# machine's details travel in the URL fragment, which the browser never sends
# to the server.
#
# Run it with: curl -fsSL debout.sybers.fr/linux | sh

# Messages are printf formats, so each language places the interface name where it reads well,
# and French messages use the typographic apostrophe on purpose.
# shellcheck disable=SC2059,SC1112

DEBOUT_SITE_URL=${DEBOUT_SITE_URL:-https://debout.sybers.fr}
DEBOUT_SYS_NET=${DEBOUT_SYS_NET:-/sys/class/net}

have() {
    command -v "$1" >/dev/null 2>&1
}

# Prints "en" or "fr" for a locale such as "fr_FR.UTF-8".
debout_language() {
    case $1 in
        fr | fr[_-]*) echo fr ;;
        *) echo en ;;
    esac
}

load_messages() {
    if [ "$1" = fr ]; then
        M_TITLE='Debout : appairer cette machine'
        M_NO_INTERFACE='Aucune interface réseau connectée avec une adresse IPv4. Connecte cette machine à ton réseau local et réessaie.'
        M_INTERFACE='Interface'
        M_NAME='Nom'
        M_CHECKING='Vérification du Wake-on-LAN...'
        M_ALL_GOOD='Le Wake-on-LAN par magic packet est activé sur %s.'
        M_WIFI='Cette machine est en Wi-Fi. Le Wake-on-LAN marche rarement en Wi-Fi : branche un câble Ethernet si tu peux.'
        M_WOL_OFF='Le Wake-on-LAN par magic packet est désactivé sur %s.'
        M_WOL_UNSUPPORTED='L’interface %s ne sait pas réveiller la machine sur un magic packet.'
        M_WOL_UNKNOWN='Impossible de lire la configuration Wake-on-LAN de %s. Vérifie-la avec : sudo ethtool %s'
        M_NO_ETHTOOL='ethtool n’est pas installé : impossible de vérifier le Wake-on-LAN. Installe-le avec ton gestionnaire de paquets et relance ce script.'
        M_BIOS='Vérifie aussi que le Wake-on-LAN est activé dans le BIOS/UEFI : ce script ne peut pas le voir.'
        M_FIX_INTRO='Debout peut corriger ça, en administrateur (sudo) :'
        M_FIX_NM='Activer le Wake-on-LAN sur la connexion NetworkManager « %s », de façon permanente'
        M_FIX_ETHTOOL='Activer le Wake-on-LAN sur %s'
        M_FIX_NOT_PERSISTENT='Sans NetworkManager, ce réglage peut se perdre au redémarrage. Rends-le permanent dans la configuration réseau de ta distribution (WakeOnLan=magic avec systemd-networkd, wakeonlan: true avec netplan).'
        M_FIX_CONFIRM='Appliquer ces changements ? [o/N]'
        M_FIX_APPLIED='Changements appliqués.'
        M_FIX_SKIPPED='Rien n’a été modifié.'
        M_FIX_FAILED='Les changements n’ont pas pu être appliqués.'
        M_SCAN_TERMINAL='Scanne ce QR code avec Debout :'
        M_OPENING='Ouverture de la page d’appairage. Scanne le QR code avec Debout :'
        M_OPEN_ELSEWHERE='Pas de navigateur ici : ouvre cette adresse sur n’importe quel ordinateur, ou installe qrencode et relance ce script pour voir le QR code dans le terminal :'
    else
        M_TITLE='Debout: pair this machine'
        M_NO_INTERFACE='No connected network interface with an IPv4 address. Connect this machine to your local network and try again.'
        M_INTERFACE='Interface'
        M_NAME='Name'
        M_CHECKING='Checking Wake-on-LAN...'
        M_ALL_GOOD='Wake-on-LAN by magic packet is on for %s.'
        M_WIFI='This machine uses Wi-Fi. Wake-on-LAN rarely works over Wi-Fi: plug in an Ethernet cable if you can.'
        M_WOL_OFF='Wake-on-LAN by magic packet is off for %s.'
        M_WOL_UNSUPPORTED='The interface %s cannot wake the machine on a magic packet.'
        M_WOL_UNKNOWN='Could not read the Wake-on-LAN settings of %s. Check them with: sudo ethtool %s'
        M_NO_ETHTOOL='ethtool is not installed, so Wake-on-LAN cannot be checked. Install it with your package manager and run this script again.'
        M_BIOS='Also check that Wake-on-LAN is enabled in the BIOS/UEFI: this script cannot see it.'
        M_FIX_INTRO='Debout can fix this, as administrator (sudo):'
        M_FIX_NM='Turn on Wake-on-LAN for the NetworkManager connection "%s", permanently'
        M_FIX_ETHTOOL='Turn on Wake-on-LAN for %s'
        M_FIX_NOT_PERSISTENT='Without NetworkManager, this setting may not survive a reboot. Make it permanent in your distribution'\''s network configuration (WakeOnLan=magic with systemd-networkd, wakeonlan: true with netplan).'
        M_FIX_CONFIRM='Apply these changes? [y/N]'
        M_FIX_APPLIED='Changes applied.'
        M_FIX_SKIPPED='Nothing changed.'
        M_FIX_FAILED='The changes could not be applied.'
        M_SCAN_TERMINAL='Scan this QR code with Debout:'
        M_OPENING='Opening the pairing page. Scan the QR code with Debout:'
        M_OPEN_ELSEWHERE='No browser here: open this address on any computer, or install qrencode and run this script again to see the QR code in the terminal:'
    fi
}

# Reads `ip -o -4 addr show scope global` on stdin, prints "interface|ip" lines.
parse_addresses() {
    awk '{ split($4, address, "/"); print $2 "|" address[1] }'
}

# Reads `ip route show default` on stdin, prints the interfaces that reach a router.
parse_default_interfaces() {
    awk '{ for (i = 1; i < NF; i++) if ($i == "dev") print $(i + 1) }'
}

# Virtual interfaces have no device in sysfs: bridges, containers, VPNs.
is_physical() {
    [ -e "$DEBOUT_SYS_NET/$1/device" ]
}

is_wireless() {
    [ -e "$DEBOUT_SYS_NET/$1/wireless" ] || [ -e "$DEBOUT_SYS_NET/$1/phy80211" ]
}

# select_interface DEFAULT_INTERFACES: reads "interface|ip" lines on stdin, prints the
# line to pair. Reaching the router beats a direct link, then wired beats Wi-Fi.
select_interface() {
    best=
    best_rank=0
    while IFS='|' read -r interface ip; do
        is_physical "$interface" || continue
        rank=1
        case " $1 " in *" $interface "*) rank=$((rank + 2)) ;; esac
        is_wireless "$interface" || rank=$((rank + 1))
        if [ "$rank" -gt "$best_rank" ]; then
            best="$interface|$ip"
            best_rank=$rank
        fi
    done
    [ -z "$best" ] || echo "$best"
}

# Reads `ethtool INTERFACE` on stdin, prints on, off, unsupported, or nothing when
# the settings are hidden (ethtool needs root for them on most drivers).
parse_wake_on() {
    awk -F': ' '
        /^[[:space:]]*Supports Wake-on:/ { supports = $2 }
        /^[[:space:]]*Wake-on:/ { current = $2 }
        END {
            if (supports == "") exit
            if (index(supports, "g") == 0) print "unsupported"
            else if (index(current, "g") > 0) print "on"
            else print "off"
        }
    '
}

wake_on_status() {
    status=$(ethtool "$1" 2>/dev/null | parse_wake_on)
    if [ -z "$status" ] && have sudo; then
        status=$(sudo -n ethtool "$1" 2>/dev/null | parse_wake_on)
    fi
    echo "$status"
}

# Prints the NetworkManager connection of the interface, or nothing.
networkmanager_connection() {
    have nmcli || return 0
    nmcli -g GENERAL.CONNECTION device show "$1" 2>/dev/null | sed -n 1p
}

# Percent-encodes everything but RFC 3986 unreserved characters, byte by byte.
url_encode() {
    printf '%s' "$1" | od -An -v -tu1 | awk '{
        for (i = 1; i <= NF; i++) {
            c = $i
            if ((c >= 48 && c <= 57) || (c >= 65 && c <= 90) || (c >= 97 && c <= 122) || c == 45 || c == 46 || c == 95 || c == 126)
                printf "%c", c
            else
                printf "%%%02X", c
        }
    }'
}

format_mac() {
    echo "$1" | tr 'abcdef' 'ABCDEF'
}

# pairing_query MAC IP NAME HOST: the payload after "debout://". HOST may be empty.
pairing_query() {
    query="v1?mac=$(url_encode "$1")&ip=$(url_encode "$2")&name=$(url_encode "$3")&os=linux"
    [ -z "$4" ] || query="$query&host=$(url_encode "$4")"
    echo "$query"
}

is_yes() {
    case $(echo "$1" | tr '[:upper:]' '[:lower:]' | tr -d '[:space:]') in
        y | yes | o | oui) return 0 ;;
        *) return 1 ;;
    esac
}

# stdin is the script itself under `curl | sh`: questions go to the terminal.
ask() {
    printf '%s ' "$1" >/dev/tty 2>/dev/null || return 1
    read -r answer </dev/tty || answer=
    echo "$answer"
}

has_browser() {
    [ -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ] && have xdg-open
}

main() {
    load_messages "$(debout_language "${LC_ALL:-${LC_MESSAGES:-${LANG:-}}}")"
    printf '\n%s\n\n' "$M_TITLE"

    selected=$(ip -o -4 addr show scope global | parse_addresses |
        select_interface "$(ip route show default | parse_default_interfaces | tr '\n' ' ')")
    if [ -z "$selected" ]; then
        echo "$M_NO_INTERFACE" >&2
        exit 1
    fi
    interface=${selected%%|*}
    ip=${selected#*|}
    mac=$(format_mac "$(cat "$DEBOUT_SYS_NET/$interface/address")")
    name=$(hostname 2>/dev/null || uname -n)
    host=
    if have systemctl && systemctl is-active --quiet avahi-daemon 2>/dev/null; then
        host="$name.local"
    fi

    printf '%-10s %s\n' "$M_INTERFACE" "$interface"
    printf '%-10s %s\n' MAC "$mac"
    printf '%-10s %s\n' IP "$ip"
    printf '%-10s %s\n\n' "$M_NAME" "$name"

    echo "$M_CHECKING"
    is_wireless "$interface" && echo "  ! $M_WIFI"
    status=
    if have ethtool; then
        status=$(wake_on_status "$interface")
        case $status in
            on) echo "  $(printf "$M_ALL_GOOD" "$interface")" ;;
            off) echo "  ! $(printf "$M_WOL_OFF" "$interface")" ;;
            unsupported) echo "  ! $(printf "$M_WOL_UNSUPPORTED" "$interface")" ;;
            *) echo "  ! $(printf "$M_WOL_UNKNOWN" "$interface" "$interface")" ;;
        esac
    else
        echo "  ! $M_NO_ETHTOOL"
    fi
    echo "  $M_BIOS"
    echo

    if [ "$status" = off ]; then
        connection=$(networkmanager_connection "$interface")
        echo "$M_FIX_INTRO"
        if [ -n "$connection" ]; then
            echo "  - $(printf "$M_FIX_NM" "$connection")"
        else
            echo "  - $(printf "$M_FIX_ETHTOOL" "$interface")"
            echo "  $M_FIX_NOT_PERSISTENT"
        fi
        if is_yes "$(ask "$M_FIX_CONFIRM")"; then
            # NetworkManager applies its setting on the next activation: ethtool covers the meantime.
            if sudo ethtool -s "$interface" wol g &&
                { [ -z "$connection" ] || sudo nmcli connection modify "$connection" 802-3-ethernet.wake-on-lan magic; }; then
                echo "$M_FIX_APPLIED"
            else
                echo "$M_FIX_FAILED" >&2
            fi
        else
            echo "$M_FIX_SKIPPED"
        fi
        echo
    fi

    query=$(pairing_query "$mac" "$ip" "$name" "$host")
    url="${DEBOUT_SITE_URL%/}/pair/#$query"
    if have qrencode; then
        echo "$M_SCAN_TERMINAL"
        qrencode -t ansiutf8 "debout://$query"
    elif has_browser; then
        echo "$M_OPENING"
        echo "  $url"
        xdg-open "$url" >/dev/null 2>&1 &
    else
        echo "$M_OPEN_ELSEWHERE"
        echo "  $url"
    fi
}

# Sourcing with DEBOUT_PAIR_SOURCED=1 (as the tests do) only loads the functions.
[ "${DEBOUT_PAIR_SOURCED:-}" = 1 ] || main "$@"
