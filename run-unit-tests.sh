#!/bin/bash
# Run AI plugin tests from the CLI.
#
# The plugin has TWO test modules, on two different ADR-020 runtime tiers, and the
# difference is the whole point — see
# ../iDempiereCLDE/docs/adr/ADR-020-test-categorization-runtime-tiers.md.
#
#   com.cloudempiere.ai.test.unit   tier U   packaging=jar + maven-surefire
#                                           JVM only. Runs on every CI build.
#                                           Carries NO @Tag: in a tier-U module the
#                                           module IS the tag.
#
#   com.cloudempiere.ai.test        tier D   eclipse-test-plugin, testRuntime=p2Installed
#                                           Equinox + p2 director + a seeded database.
#                                           skipTests defaults to true, so a green
#                                           `mvn verify` here proves nothing ran.
#                                           All 14 of its unit-scope tests also carry
#                                           @Tag("needs-runtime"): each was tried in the jar
#                                           module and failed there — 6 of 20 candidates
#                                           survived.
#
# This script drives Maven. It used to hand-build a javac/JUnit-console classpath out of
# ../iDempiereCLDE and the p2 repository; that is gone. The hand-built classpath was a
# half-runtime — enough org.adempiere.base on it to load hosts that a real tier-U module
# cannot load — so it disagreed with CI in both directions.
#
# Usage:
#   ./run-unit-tests.sh                          tier U — every test in .test.unit
#   ./run-unit-tests.sh AIRequestTest            tier U — one class
#   ./run-unit-tests.sh 'Anthropic*'             tier U — a surefire -Dtest pattern
#   ./run-unit-tests.sh --host                   rebuild + install the host jars, then tier U
#   ./run-unit-tests.sh --runtime                tier D — the fragment, via Tycho (needs a DB)
#   ./run-unit-tests.sh --runtime --group e2e    tier D — a different tag selection
#   ./run-unit-tests.sh -v                       do not trim Maven's output
#   ./run-unit-tests.sh --list                   list what each module holds, run nothing
#
# Env:
#   MAVEN_REPO_LOCAL  local repository (default $HOME/.m2/repository-idempiere-10)
#                     Write $HOME, never ~ — zsh does not expand a tilde after '=' in
#                     -Dmaven.repo.local=, and Maven then silently creates ./~ and reports
#                     the host jar as missing.
#   COMPOSITE_URL     escape hatch for --host, unset by default. Prefer --p2; setting this beats
#                     --p2 for that one repository, which is the vintage-mixing the script
#                     otherwise avoids, so it warns when combined with --p2 local.
#   IDEMPIERE_HOME    same as --idempiere-home below; the flag wins if both are given.
#
# --idempiere-home PATH   --runtime only. Becomes -Didempiere.home, which is the property the
#   fragment's <argLine> interpolates. NOT -Dtycho.testArgLine: the fragment sets <argLine>
#   explicitly, and an explicit argLine beats that property, so -Dtycho.testArgLine is silently
#   ignored (and would also wipe the ${p1}..${p5} parallel-execution flags if it did apply).
#   The path is resolved to an absolute one, because the pom default is relative to the
#   FRAGMENT directory, not to where you are standing.
#
# --p2 <local|s3>   which p2 repositories to resolve against. Omit to inherit ~/.m2/settings.xml.
#   local  activate cloudempiere-local AND pin cloudempiere.workspace to this checkout. The pom
#          default is $HOME/github/idempiere-cloudempiere, which is not where anyone checks out,
#          so a bare -P aims the file:// repos at a missing directory.
#   s3     deactivate it, resolving from S3 exactly as CI does.
#
# Every run ends with a machine-readable RESULT line per tier; that plus the exit status is the
# contract. Exit: 0 pass, 1 build/test failure, 2 bad usage, 3 a -Dtest filter matching nothing.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

UNIT_MODULE=com.cloudempiere.ai.test.unit
FRAGMENT=com.cloudempiere.ai.test
HOST=com.cloudempiere.ai.core

REPO_LOCAL="${MAVEN_REPO_LOCAL:-$HOME/.m2/repository-idempiere-10}"
# Opt-in ONLY. This used to hardcode the S3 staging composite, so every --host silently
# overrode whatever --p2 or settings.xml had decided: a CLI -D beats a profile property, so
# `--p2 local` would have built against file:// repos plus one remote one.
COMPOSITE_URL="${COMPOSITE_URL:-}"

VERBOSE=false
BUILD_HOST=false
MODE=unit
GROUP=""
TEST_FILTER=""
P2=""

# The workspace root holding this repo alongside iDempiereCLDE. --p2 local must pin it: the
# parent pom's own default is $HOME/github/idempiere-cloudempiere, so activating the profile
# with a bare -P silently aims the file:// p2 repositories at a directory that does not exist.
WORKSPACE="$(cd "$SCRIPT_DIR/.." && pwd)"

while [[ $# -gt 0 ]]; do
    case "$1" in
        -v|--verbose) VERBOSE=true; shift ;;
        --host)       BUILD_HOST=true; shift ;;
        --runtime)    MODE=runtime; shift ;;
        --group)      GROUP="${2:?--group needs a tag}"; shift 2 ;;
        --p2)         P2="${2:?--p2 needs local or s3}"; shift 2 ;;
        --idempiere-home) IDEMPIERE_HOME="${2:?--idempiere-home needs a path}"; shift 2 ;;
        --list)       MODE=list; shift ;;
        -h|--help)    sed -n '2,/^set -euo/p' "$0" | sed 's/^# \{0,1\}//; $d'; exit 0 ;;
        -*)           echo "unknown option: $1 (try --help)" >&2; exit 2 ;;
        *)            TEST_FILTER="$1"; shift ;;
    esac
done

MVN=(mvn -B "-Dmaven.repo.local=$REPO_LOCAL")
# -q keeps Maven's reactor INFO out of the way but is NOT enough on its own, and used to be the
# whole strategy, which failed in both directions: in tier U it also hid surefire's "Tests run:"
# line, so a green run printed nothing; in tier D it hid nothing that mattered, because
# tycho-surefire forks an Equinox JVM whose stdout never passes through Maven's logger. The
# RESULT line below now carries the numbers, and the forked stream goes to a file.
$VERBOSE || MVN+=(-q)

# Which p2 repositories to resolve against. Applied to every Maven invocation, not just
# --runtime, so --host resolves from the same place the tests will.
case "$P2" in
    "")     ;;
    local)  MVN+=(-P cloudempiere-local "-Dcloudempiere.workspace=$WORKSPACE") ;;
    # -P '!id' deactivates regardless of activation source, including settings.xml.
    s3)     MVN+=(-P '!cloudempiere-local') ;;
    *)      echo "--p2 takes 'local' or 's3', not '$P2'" >&2; exit 2 ;;
esac

# Tier D boots iDempiere inside a forked JVM and emits ~1000 lines of legitimate startup
# logging that no regex can safely strip -- real diagnostics are mixed in. So the full stream
# goes to a file and only an allowlist reaches the terminal. Not under target/: --runtime runs
# clean, which would delete it mid-write.
LOGFILE="$SCRIPT_DIR/.run-unit-tests.log"
: > "$LOGFILE" 2>/dev/null || true   # truncate once per run; run_mvn appends, so a mode that
                                     # invokes Maven more than once keeps every call's output
KEEP='Tests run:|^Results:|\[ERROR\]|\[WARNING\]|BUILD (SUCCESS|FAILURE)|^Total time|<<< (FAILURE|ERROR)|^==>'

run_mvn() {
    if $VERBOSE; then
        "${MVN[@]}" "$@"
        return
    fi
    # 2>&1 is load-bearing: java.util.logging writes to stderr, so the forked JVM's output
    # bypasses a stdout-only pipe. pipefail is disabled here on purpose -- grep exits 1 when it
    # matches nothing, which would read as a failed build; Maven's status comes from PIPESTATUS.
    set +o pipefail
    # The `|| true` is required, not cosmetic: grep exits 1 when it matches nothing, which is
    # a perfectly normal quiet build (a small -N install emits no line the allowlist wants).
    # Without it, set -e kills the script on the pipeline's status before PIPESTATUS is read,
    # and the run dies silently with no output and no RESULT line.
    "${MVN[@]}" "$@" 2>&1 | tee -a "$LOGFILE" | { grep -E "$KEEP" || true; }
    local st=${PIPESTATUS[0]}
    set -o pipefail
    return $st
}

# ---------------------------------------------------------------- list
if [ "$MODE" = list ]; then
    echo "tier U — $UNIT_MODULE (jar, runs in CI, no tags):"
    find "$UNIT_MODULE/src/test/java" -name '*Test.java' 2>/dev/null \
        | sed "s|$UNIT_MODULE/src/test/java/||; s|/|.|g; s|\.java$||; s|^|  |" | sort
    echo
    echo "tier D — $FRAGMENT (eclipse-test-plugin, p2Installed, skipTests=true by default):"
    for tag in unit needs-runtime integration e2e slow; do
        printf '  @Tag("%s")%*s%s files\n' "$tag" $((16 - ${#tag})) '' \
            "$(grep -rl "@Tag(\"$tag\")" "$FRAGMENT/src" 2>/dev/null | wc -l | tr -d ' ')"
    done
    exit 0
fi

# ---------------------------------------------------------------- host jar
# The tier-U module resolves the host by GAV from the local repository, not from a reactor,
# so a stale install is invisible until javac fails on a signature that has since changed.
#
# Version schemes disagree inside this repo — ai.core's pom is 0.32.0-SNAPSHOT while
# ai.parent is 10.0.2-SNAPSHOT and the MANIFEST says 0.32.0.qualifier. Read the version the
# tier-U pom itself declares; it is the one that must resolve.
VERSION="$(sed -n 's|.*<version>\(.*\)</version>.*|\1|p' "$UNIT_MODULE/pom.xml" | head -1)"

build_host() {
    # -am does NOT follow OSGi requirements, so ai.deps has to be named explicitly:
    # ai.core Require-Bundles it, but no pom dependency records that.
    echo "==> building + installing $HOST $VERSION (+ ai.deps, which -am would miss)"
    local args=(-f pom.xml install -DskipTests
        -pl com.cloudempiere.ai.parent,com.cloudempiere.ai.deps,com.cloudempiere.ai.core)
    if [ -n "$COMPOSITE_URL" ]; then
        [ "$P2" = local ] && echo "NOTE: COMPOSITE_URL overrides the URL --p2 local sets."
        args+=("-Dcloudempiere.composite.repository.url=$COMPOSITE_URL")
    fi
    run_mvn "${args[@]}"
}

if [ "$MODE" = runtime ]; then
    :   # the reactor build below produces the hosts; installed jars are irrelevant here
elif $BUILD_HOST; then
    build_host
else
    # Coordinates come from the tier-U pom, never from a hardcoded com/cloudempiere/<artifact>
    # path: the groupId is not uniform across these repos (com.cloudempiere here,
    # com.cloudempiere.ai in ai, com.cloudempiere.cache.extensions in cache), so a hardcoded
    # path reported every jar as missing however many were actually installed.
    while IFS=$'\t' read -r jar art; do
        if [ ! -f "$jar" ]; then
            echo "ERROR: host jar not installed: $jar" >&2
            echo "       run: $0 --host" >&2
            exit 1
        fi
        newer="$(find "$art/src" -name '*.java' -newer "$jar" -print -quit 2>/dev/null || true)"
        if [ -n "$newer" ]; then
            echo "WARNING: $art source is newer than its installed jar."
            echo "         first newer file: $newer"
            echo "         a compile error below is probably staleness, not a real break."
            echo "         re-run with --host to rebuild."
            echo
        fi
    done < <(python3 "$SCRIPT_DIR/.host-jars.py" "$UNIT_MODULE/pom.xml" "$REPO_LOCAL")
fi

# ---------------------------------------------------------------- reporting
# One machine-readable line per tier, from an EXIT trap so it survives a failing build too.
# Callers (and agents) key off this and the exit status, never off Maven's phrasing, which
# differs between maven-surefire (tier U) and tycho-surefire in a forked Equinox (tier D):
#   RESULT tier=U tests=180 failures=0 errors=0 skipped=0
#
# STAMP is created just before Maven runs; only reports newer than it count. Without that,
# `test` (which does not clean) leaves the previous run's XML in place and a -Dtest filter that
# matched nothing gets summarised as a full green pass.
STAMP="$(mktemp -t run-unit-tests.XXXXXX)"

FRESH_TOTAL=0
summarize() {
    local tier="$1" dir="$2" out
    if [ ! -d "$dir" ]; then
        echo "RESULT tier=$tier status=no-reports  (nothing ran; the build failed before tests)"
        return
    fi
    out="$(python3 "$SCRIPT_DIR/.surefire-summary.py" "$tier" "$dir" "$STAMP")"
    echo "$out"
    echo "       reports: $dir/"
    case "$out" in *" tests="*) FRESH_TOTAL=1 ;; esac
}

finish() {
    local code=$?
    $VERBOSE || [ ! -f "${LOGFILE:-}" ] || echo "       full log: $LOGFILE"
    case "$MODE" in
        unit)    echo; summarize U "$UNIT_MODULE/target/surefire-reports" ;;
        runtime) echo
                 summarize D "$FRAGMENT/target/surefire-reports"
                 summarize U "$UNIT_MODULE/target/surefire-reports" ;;
        *)       rm -f "$STAMP"; exit $code ;;
    esac
    # A filter matching nothing is a typo, not a pass. surefire is told not to fail on it, so
    # the script has to catch it here or it exits 0 having run nothing.
    if [ "$code" = 0 ] && [ -n "$TEST_FILTER" ] && [ "$FRESH_TOTAL" = 0 ]; then
        echo "ERROR: no test matched $TEST_FILTER — nothing ran." >&2
        code=3
    fi
    rm -f "$STAMP"
    exit $code
}

# ---------------------------------------------------------------- run
trap finish EXIT
touch "$STAMP"
case "$MODE" in
  unit)
    echo "==> tier U: $UNIT_MODULE"
    ARGS=(-f "$UNIT_MODULE/pom.xml" test)
    # surefire 3.x renamed this. The bare failIfNoSpecifiedTests is the surefire-2 spelling and
    # is silently ignored, so a typo'd class name died with a stack trace and a [Help 1] URL.
    [ -n "$TEST_FILTER" ] && ARGS+=("-Dtest=$TEST_FILTER" -Dsurefire.failIfNoSpecifiedTests=false)
    run_mvn "${ARGS[@]}"
    ;;
  runtime)
    # FROM THE REACTOR ROOT, not -f $FRAGMENT/pom.xml. The fragment's Fragment-Host is resolved
    # from the target platform, and a standalone build has no reactor to supply the freshly
    # built host -- it resolves a published one, or fails outright. In the reactor Tycho hands
    # the just-built bundle to the p2 director instead.
    #
    # `clean` because the director provisions from target/work; a stale one silently re-runs the
    # previous bundle. `install`, not `verify`: install writes the host jar into $REPO_LOCAL,
    # which is what the bare tier-U path resolves by GAV, so --runtime is a strict superset of
    # --host instead of leaving the fast path stale.
    #
    # No composite override: settings.xml's cloudempiere-local rewrites several repository URLs
    # at once, and overriding one of them mixes a remote vintage into local ones.
    echo "==> tier D: $FRAGMENT via the reactor (Equinox + p2 director)"
    echo "    tier D needs a seeded database; set IDEMPIERE_HOME if the default is wrong."
    ARGS=(-f pom.xml clean install -DskipTests=false)
    [ -n "$GROUP" ] && ARGS+=("-Dtest.groups=$GROUP" -Dtest.excludedGroups=)
    [ -n "$TEST_FILTER" ] && ARGS+=("-Dtest=$TEST_FILTER" -Dsurefire.failIfNoSpecifiedTests=false)
    # The fragment hardcodes <argLine>-DIDEMPIERE_HOME=${idempiere.home} ...</argLine>, so an
    # explicit argLine wins over tycho.testArgLine and idempiere.home is the only live knob.
    # Made absolute: the pom's default is relative to the FRAGMENT directory, so a path a
    # user typed relative to the repo root would silently resolve somewhere else.
    if [ -n "${IDEMPIERE_HOME:-}" ]; then
        IH="$(cd "$IDEMPIERE_HOME" 2>/dev/null && pwd)" || {
            echo "--idempiere-home: no such directory: $IDEMPIERE_HOME" >&2; exit 2; }
        [ -f "$IH/idempiere.properties" ] || echo "WARNING: no idempiere.properties in $IH"
        ARGS+=("-Didempiere.home=$IH")
    fi
    run_mvn "${ARGS[@]}"
    ;;
esac
