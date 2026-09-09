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
#   COMPOSITE_URL     p2 composite for --host/--runtime. Defaults to the S3 staging composite,
#                     because the `cloudempiere-local` mirror that ~/.m2/settings.xml activates
#                     points at iDempiereCLDE/_composite, which is not built in this workspace.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

UNIT_MODULE=com.cloudempiere.ai.test.unit
FRAGMENT=com.cloudempiere.ai.test
HOST=com.cloudempiere.ai.core

REPO_LOCAL="${MAVEN_REPO_LOCAL:-$HOME/.m2/repository-idempiere-10}"
COMPOSITE_URL="${COMPOSITE_URL:-https://cloudempiere-p2.s3.eu-west-1.amazonaws.com/plugins/com.cloudempiere.composite/staging/latest/}"

VERBOSE=false
BUILD_HOST=false
MODE=unit
GROUP=""
TEST_FILTER=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        -v|--verbose) VERBOSE=true; shift ;;
        --host)       BUILD_HOST=true; shift ;;
        --runtime)    MODE=runtime; shift ;;
        --group)      GROUP="${2:?--group needs a tag}"; shift 2 ;;
        --list)       MODE=list; shift ;;
        -h|--help)    sed -n '2,/^set -euo/p' "$0" | sed 's/^# \{0,1\}//; $d'; exit 0 ;;
        -*)           echo "unknown option: $1 (try --help)" >&2; exit 2 ;;
        *)            TEST_FILTER="$1"; shift ;;
    esac
done

MVN=(mvn -B "-Dmaven.repo.local=$REPO_LOCAL")
$VERBOSE || MVN+=(-q)

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
INSTALLED="$REPO_LOCAL/com/cloudempiere/$HOST/$VERSION/$HOST-$VERSION.jar"

build_host() {
    # -am does NOT follow OSGi requirements, so ai.deps has to be named explicitly:
    # ai.core Require-Bundles it, but no pom dependency records that.
    echo "==> building + installing $HOST $VERSION (+ ai.deps, which -am would miss)"
    "${MVN[@]}" -f pom.xml install -DskipTests \
        -pl com.cloudempiere.ai.parent,com.cloudempiere.ai.deps,com.cloudempiere.ai.core \
        "-Dcloudempiere.composite.repository.url=$COMPOSITE_URL"
}

if $BUILD_HOST; then
    build_host
elif [ ! -f "$INSTALLED" ]; then
    echo "ERROR: host jar not installed: $INSTALLED" >&2
    echo "       run: $0 --host" >&2
    exit 1
else
    NEWER="$(find "$HOST/src" -name '*.java' -newer "$INSTALLED" -print -quit 2>/dev/null || true)"
    if [ -n "$NEWER" ]; then
        echo "WARNING: $HOST source is newer than the installed jar."
        echo "         first newer file: $NEWER"
        echo "         a compile error below is probably staleness, not a real break."
        echo "         re-run with --host to rebuild."
        echo
    fi
fi

# ---------------------------------------------------------------- run
case "$MODE" in
  unit)
    echo "==> tier U: $UNIT_MODULE"
    ARGS=(-f "$UNIT_MODULE/pom.xml" test)
    [ -n "$TEST_FILTER" ] && ARGS+=("-Dtest=$TEST_FILTER" -DfailIfNoSpecifiedTests=false)
    "${MVN[@]}" "${ARGS[@]}"
    echo
    echo "surefire reports: $UNIT_MODULE/target/surefire-reports/"
    ;;
  runtime)
    echo "==> tier D: $FRAGMENT (Equinox + p2 director + seeded DB)"
    echo "    tier D needs IDEMPIERE_HOME to point at a built core with a seeded database;"
    echo "    without it the p2 tests start and then fail on the first DB access."
    ARGS=(-f "$FRAGMENT/pom.xml" verify -DskipTests=false
          "-Dcloudempiere.composite.repository.url=$COMPOSITE_URL")
    [ -n "$GROUP" ] && ARGS+=("-Dtest.groups=$GROUP" -Dtest.excludedGroups=)
    [ -n "$TEST_FILTER" ] && ARGS+=("-Dtest=$TEST_FILTER")
    [ -n "${IDEMPIERE_HOME:-}" ] && ARGS+=("-Dtycho.testArgLine=-DIDEMPIERE_HOME=$IDEMPIERE_HOME")
    "${MVN[@]}" "${ARGS[@]}"
    ;;
esac
