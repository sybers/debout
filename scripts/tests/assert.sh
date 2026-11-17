# Minimal assertions for the POSIX sh pairing scripts, sourced by each *.test.sh.

TESTS=0
FAILURES=0

# check NAME EXPECTED ACTUAL
check() {
    TESTS=$((TESTS + 1))
    if [ "$2" != "$3" ]; then
        FAILURES=$((FAILURES + 1))
        printf 'FAIL %s\n  expected: %s\n  actual:   %s\n' "$1" "$2" "$3"
    fi
}

# check_contains NAME NEEDLE HAYSTACK
check_contains() {
    TESTS=$((TESTS + 1))
    case $3 in
        *"$2"*) ;;
        *)
            FAILURES=$((FAILURES + 1))
            printf 'FAIL %s\n  expected to contain: %s\n  actual: %s\n' "$1" "$2" "$3"
            ;;
    esac
}

finish() {
    printf '%s: %d checks, %d failed\n' "$1" "$TESTS" "$FAILURES"
    [ "$FAILURES" -eq 0 ]
}
