#!/usr/bin/env python3
"""Sum a surefire-reports directory into one machine-readable line.

Kept out of run-unit-tests.sh so the shell has no embedded heredoc, and so the same
summary can be reused by CI. Reads the XML rather than scraping stdout: tier U runs under
maven-surefire and tier D under tycho-surefire in a forked Equinox JVM, and their console
formats differ, but both emit the same TEST-*.xml attributes.

Note on the number: this is the sum over per-class reports, which is what the .txt files and
the console's own per-class "Tests run:" lines also add up to. Surefire's aggregate "Results:"
banner can read slightly higher -- 496 vs 490 on the tier-D fragment -- because 28 of those
classes use @Nested and the banner counts nested containers that the per-class reports do not.
The per-class sum is the reproducible one: it can be recomputed from the artifacts after the
fact, which the banner cannot, so that is what RESULT reports.
"""
import sys, glob, xml.etree.ElementTree as ET

import os
tier, d = sys.argv[1], sys.argv[2]
# Optional third arg: a stamp file created immediately before the build. Only reports written
# after it count, so a stale directory from an earlier run is never mistaken for this one's
# result -- surefire's `test` phase does not clean, so that directory usually still exists.
since = os.path.getmtime(sys.argv[3]) if len(sys.argv) > 3 and os.path.exists(sys.argv[3]) else 0
totals = dict(tests=0, failures=0, errors=0, skipped=0)
files = [x for x in glob.glob(d + "/TEST-*.xml") if os.path.getmtime(x) >= since]
bad = 0
for x in files:
    try:
        r = ET.parse(x).getroot()
    except Exception:
        bad += 1
        continue
    for k in totals:
        totals[k] += int(r.get(k, 0) or 0)

if not files:
    print(f"RESULT tier={tier} status=no-tests-ran")
    sys.exit(0)

line = (f"RESULT tier={tier} tests={totals['tests']} failures={totals['failures']} "
        f"errors={totals['errors']} skipped={totals['skipped']}")
if bad:
    line += f" unreadable={bad}"
print(line)
