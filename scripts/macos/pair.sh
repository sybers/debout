#!/bin/sh
# Debout: pair this Mac with the Debout Android app.
#
# Finds the active network interface (Ethernet first), checks "Wake for network
# access" and offers to turn it on, then opens a page that shows a QR code to scan
# with Debout. The Mac's details travel in the URL fragment, which the browser
# never sends to the server.
#
# Run it with: curl -fsSL debout.sybers.fr/mac | sh

DEBOUT_SITE_URL=${DEBOUT_SITE_URL:-https://debout.sybers.fr}

# Prints "en" or "fr" for a locale such as "fr_FR.UTF-8" or "fr-CA".
debout_language() {
    case $1 in
        fr | fr[_-]*) echo fr ;;
        *) echo en ;;
    esac
}

# The interface language comes from AppleLanguages; Terminal may not export LANG.
system_locale() {
    first=$(defaults read -g AppleLanguages 2>/dev/null | sed -n 's/^[[:space:]]*"\{0,1\}\([A-Za-z][A-Za-z_-]*\).*/\1/p' | sed -n 1p)
    echo "${first:-${LC_ALL:-${LC_MESSAGES:-${LANG:-}}}}"
}

load_messages() {
    if [ "$1" = fr ]; then
        M_TITLE='Debout : appairer ce Mac'
        M_NO_INTERFACE='Aucune interface réseau connectée avec une adresse IPv4. Connecte ce Mac à ton réseau local et réessaie.'
        M_INTERFACE='Interface'
        M_NAME='Nom'
        M_CHECKING='Vérification du Wake-on-LAN...'
        M_ALL_GOOD='« Réveiller pour l’accès au réseau » est activé.'
        M_WIFI='Ce Mac est en Wi-Fi. Le Wake-on-LAN marche rarement en Wi-Fi : branche un câble Ethernet si tu peux.'
        M_WOMP_OFF='« Réveiller pour l’accès au réseau » est désactivé.'
        M_WOMP_UNSUPPORTED='Ce Mac ne propose pas « Réveiller pour l’accès au réseau ».'
        M_SLEEP_NOTE='Un Mac se réveille depuis la veille, pas depuis un arrêt complet : mets-le en veille plutôt que de l’éteindre. Même ainsi, le réveil n’est pas toujours fiable sur Mac.'
        M_FIX_INTRO='Debout peut corriger ça, en administrateur (sudo) :'
        M_FIX_WOMP='Activer « Réveiller pour l’accès au réseau »'
        M_FIX_CONFIRM='Appliquer ce changement ? [o/N]'
        M_FIX_APPLIED='Changement appliqué.'
        M_FIX_SKIPPED='Rien n’a été modifié.'
        M_FIX_FAILED='Le changement n’a pas pu être appliqué.'
        M_OPENING='Ouverture de la page d’appairage. Scanne le QR code avec Debout :'
    else
        M_TITLE='Debout: pair this Mac'
        M_NO_INTERFACE='No connected network interface with an IPv4 address. Connect this Mac to your local network and try again.'
        M_INTERFACE='Interface'
        M_NAME='Name'
        M_CHECKING='Checking Wake-on-LAN...'
        M_ALL_GOOD='"Wake for network access" is on.'
        M_WIFI='This Mac uses Wi-Fi. Wake-on-LAN rarely works over Wi-Fi: plug in an Ethernet cable if you can.'
        M_WOMP_OFF='"Wake for network access" is off.'
        M_WOMP_UNSUPPORTED='This Mac does not offer "Wake for network access".'
        M_SLEEP_NOTE='A Mac wakes from sleep, not from a full shutdown: put it to sleep rather than shutting it down. Even then, waking is not always reliable on a Mac.'
        M_FIX_INTRO='Debout can fix this, as administrator (sudo):'
        M_FIX_WOMP='Turn on "Wake for network access"'
        M_FIX_CONFIRM='Apply this change? [y/N]'
        M_FIX_APPLIED='Change applied.'
        M_FIX_SKIPPED='Nothing changed.'
        M_FIX_FAILED='The change could not be applied.'
        M_OPENING='Opening the pairing page. Scan the QR code with Debout:'
    fi
}

# Reads `networksetup -listallhardwareports` on stdin, prints "device|port|mac" lines.
parse_hardware_ports() {
    awk -F': ' '
        /^Hardware Port:/ { port = $2 }
        /^Device:/ { device = $2 }
        /^Ethernet Address:/ { if (device != "") print device "|" port "|" $2; device = "" }
    '
}

# Ports that never carry the local network: tethering, Bluetooth, bridges.
is_ignored_port() {
    case $1 in
        *iPhone* | *iPad* | *Bluetooth* | *Bridge* | *VPN*) return 0 ;;
        *) return 1 ;;
    esac
}

is_wifi_port() {
    case $1 in
        Wi-Fi | AirPort) return 0 ;;
        *) return 1 ;;
    esac
}

# Reads "device|port|mac|ip" lines on stdin, prints the line to pair: wired first, then Wi-Fi.
select_interface() {
    wired=
    wifi=
    while IFS='|' read -r device port mac ip; do
        [ -n "$ip" ] || continue
        case $ip in 169.254.*) continue ;; esac
        is_ignored_port "$port" && continue
        if is_wifi_port "$port"; then
            [ -n "$wifi" ] || wifi="$device|$port|$mac|$ip"
        else
            [ -n "$wired" ] || wired="$device|$port|$mac|$ip"
        fi
    done
    if [ -n "$wired" ]; then echo "$wired"; elif [ -n "$wifi" ]; then echo "$wifi"; fi
}

list_interfaces() {
    networksetup -listallhardwareports | parse_hardware_ports |
        while IFS='|' read -r device port mac; do
            echo "$device|$port|$mac|$(ipconfig getifaddr "$device" 2>/dev/null)"
        done
}

# Prints 1, 0, or nothing when this Mac has no such setting.
wake_on_magic_packet() {
    pmset -g | awk '$1 == "womp" { print $2 }'
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

# pairing_url SITE MAC IP NAME HOST
pairing_url() {
    printf '%s/pair/#v1?mac=%s&ip=%s&name=%s&os=macos&host=%s\n' \
        "${1%/}" "$(url_encode "$2")" "$(url_encode "$3")" "$(url_encode "$4")" "$(url_encode "$5")"
}

is_yes() {
    case $(echo "$1" | tr 'A-Z' 'a-z' | tr -d '[:space:]') in
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

main() {
    load_messages "$(debout_language "$(system_locale)")"
    printf '\n%s\n\n' "$M_TITLE"

    selected=$(list_interfaces | select_interface)
    if [ -z "$selected" ]; then
        echo "$M_NO_INTERFACE" >&2
        exit 1
    fi
    IFS='|' read -r device port mac ip <<EOF
$selected
EOF
    mac=$(format_mac "$mac")
    name=$(scutil --get ComputerName 2>/dev/null || hostname)
    host="$(scutil --get LocalHostName 2>/dev/null || hostname -s).local"

    printf '%-10s %s (%s)\n' "$M_INTERFACE" "$port" "$device"
    printf '%-10s %s\n' MAC "$mac"
    printf '%-10s %s\n' IP "$ip"
    printf '%-10s %s (%s)\n\n' "$M_NAME" "$name" "$host"

    echo "$M_CHECKING"
    is_wifi_port "$port" && echo "  ! $M_WIFI"
    womp=$(wake_on_magic_packet)
    case $womp in
        1) echo "  $M_ALL_GOOD" ;;
        0) echo "  ! $M_WOMP_OFF" ;;
        *) echo "  ! $M_WOMP_UNSUPPORTED" ;;
    esac
    echo "  $M_SLEEP_NOTE"
    echo

    if [ "$womp" = 0 ]; then
        echo "$M_FIX_INTRO"
        echo "  - $M_FIX_WOMP"
        if is_yes "$(ask "$M_FIX_CONFIRM")"; then
            if sudo pmset -a womp 1; then echo "$M_FIX_APPLIED"; else echo "$M_FIX_FAILED" >&2; fi
        else
            echo "$M_FIX_SKIPPED"
        fi
        echo
    fi

    url=$(pairing_url "$DEBOUT_SITE_URL" "$mac" "$ip" "$name" "$host")
    echo "$M_OPENING"
    echo "  $url"
    open "$url"
}

# Sourcing with DEBOUT_PAIR_SOURCED=1 (as the tests do) only loads the functions.
[ "${DEBOUT_PAIR_SOURCED:-}" = 1 ] || main "$@"
