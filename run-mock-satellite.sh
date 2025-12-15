#!/bin/bash
# Run Mock AI Hub Server for development and testing
#
# This script starts a lightweight HTTP server that simulates the
# iDempiere AI Hub Service (ADR-042).
#
# Usage:
#   ./run-mock-satellite.sh              # Start on default port 8090
#   ./run-mock-satellite.sh 8091         # Start on custom port
#   ./run-mock-satellite.sh 8090 ECHO    # Start in ECHO mode
#   ./run-mock-satellite.sh 8090 MOCK    # Start in MOCK mode (default)
#
# Endpoints:
#   POST /v1/chat/completions  - Chat completions (OpenAI-compatible)
#   POST /v1/embeddings        - Embeddings (OpenAI-compatible)
#   GET  /health               - Health check
#   GET  /info                 - Server information
#
# Configure iDempiere AIG_Provider:
#   - Type: SAT (iDempiere AI Hub)
#   - Endpoint: http://localhost:8090
#   - APIKey: mock-token (any value works)
#   - ModelName: claude-sonnet-4 (or any model name)

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Default values
PORT="${1:-8090}"
MODE="${2:-MOCK}"

# Java home
export JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/amazon-corretto-11.jdk/Contents/Home}"

echo "=== Mock AI Hub Server ==="
echo "Java: $JAVA_HOME"
echo ""

# Directories
CLASSES_DIR="$SCRIPT_DIR/target/classes"

# Check if classes are compiled
if [ ! -d "$CLASSES_DIR" ] || [ -z "$(ls -A $CLASSES_DIR 2>/dev/null)" ]; then
    echo "Classes not compiled. Running mvn compile..."
    mvn compile -DskipTests -q
fi

# Run the mock server
echo "Starting Mock AI Hub Server..."
echo "  Port: $PORT"
echo "  Mode: $MODE"
echo ""
echo "Endpoints:"
echo "  POST http://localhost:$PORT/v1/chat/completions"
echo "  POST http://localhost:$PORT/v1/embeddings"
echo "  GET  http://localhost:$PORT/health"
echo "  GET  http://localhost:$PORT/info"
echo ""
echo "Press Ctrl+C to stop..."
echo ""

"$JAVA_HOME/bin/java" \
    -cp "$CLASSES_DIR" \
    com.cloudempiere.ai.provider.satellite.MockSatelliteServer \
    "$PORT" "$MODE"
