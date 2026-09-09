#!/usr/bin/env python3
"""Print the local-repository jar path for every cloudempiere dependency of a tier-U pom.

Usage: .host-jars.py <tier-U pom> <local repo root>
Output: one "<jar path>\t<artifactId>" line per host.

Exists because the groupId is NOT uniform across these repos -- graphql uses com.cloudempiere,
ai uses com.cloudempiere.ai, cache uses com.cloudempiere.cache.extensions -- so a hardcoded
com/cloudempiere/<artifactId> path is wrong in two of the three, and the staleness gate then
reports every jar as missing no matter what is installed. Read the coordinates the tier-U
module actually declares; those are the ones Maven will resolve.
"""
import sys, os, xml.etree.ElementTree as ET

pom, repo = sys.argv[1], sys.argv[2]
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}
root = ET.parse(pom).getroot()
for d in root.findall('.//m:dependencies/m:dependency', NS):
    g = d.findtext('m:groupId', default='', namespaces=NS)
    a = d.findtext('m:artifactId', default='', namespaces=NS)
    v = d.findtext('m:version', default='', namespaces=NS)
    if not g.startswith('com.cloudempiere') or not a or not v:
        continue
    jar = os.path.join(repo, *g.split('.'), a, v, f"{a}-{v}.jar")
    print(f"{jar}\t{a}")
