#!/bin/bash
###############################################################################
# Fix Eclipse Marker Corruption
#
# Symptoms:
#   "Marker [...] is not of a displayable type"
#   Phantom errors that won't go away
#
# This script removes corrupted Eclipse markers.
###############################################################################

set -e

WORKSPACE_DIR="/Users/developer/GitHub/com.cloudempiere.ai"

echo "🔧 Fixing Eclipse marker corruption..."
echo ""

# Method 1: Delete project markers
echo "Method 1: Deleting project-level markers"
find "$WORKSPACE_DIR" -name ".markers" -type f -delete
echo "✅ Deleted .markers files"
echo ""

# Method 2: Delete Eclipse metadata cache for projects
echo "Method 2: Clearing Eclipse metadata cache"
if [ -d "$WORKSPACE_DIR/.metadata/.plugins/org.eclipse.core.resources/.projects" ]; then
    # Only delete marker-related metadata, keep project structure
    find "$WORKSPACE_DIR/.metadata/.plugins/org.eclipse.core.resources/.projects" \
         -name ".markers" -delete 2>/dev/null || true
    echo "✅ Cleared metadata markers"
else
    echo "ℹ️  No .metadata directory found (not an Eclipse workspace root)"
fi
echo ""

echo "════════════════════════════════════════════════════════════"
echo "NEXT STEPS IN ECLIPSE:"
echo "════════════════════════════════════════════════════════════"
echo "1. Close Eclipse if it's open"
echo "2. Restart Eclipse"
echo "3. Project → Clean → Clean all projects"
echo "4. Let Eclipse rebuild"
echo ""
echo "The marker errors should be gone!"
echo "════════════════════════════════════════════════════════════"
