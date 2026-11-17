#!/bin/sh
# Runs the tests of the POSIX sh pairing scripts with the given shell (default: sh).
# Usage: scripts/test.sh [shell]

shell=${1:-sh}
root=$(cd "$(dirname "$0")" && pwd)
status=0
for test in "$root"/*/pair.test.sh; do
    "$shell" "$test" || status=1
done
exit "$status"
