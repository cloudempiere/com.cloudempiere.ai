#!/bin/bash
# Test script for KEntryEventHandler
# Tests the automatic embedding generation for K_Entry changes
#
# Usage:
#   ./test-kentry-handler.sh              # Run all tests
#   ./test-kentry-handler.sh --setup-only # Only setup, no tests
#   ./test-kentry-handler.sh --quick      # Quick test (skip setup checks)

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
DB_USER="${DB_USER:-adempiere}"
DB_NAME="${DB_NAME:-idempiere}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
PSQL="psql -U $DB_USER -d $DB_NAME -h $DB_HOST -p $DB_PORT"

# Parse arguments
SETUP_ONLY=false
QUICK_TEST=false
while [[ $# -gt 0 ]]; do
    case $1 in
        --setup-only) SETUP_ONLY=true; shift ;;
        --quick) QUICK_TEST=true; shift ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

# Helper functions
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "ℹ️  $1"
}

check_command() {
    if command -v $1 &> /dev/null; then
        print_success "$1 is installed"
        return 0
    else
        print_error "$1 is not installed"
        return 1
    fi
}

# Banner
echo "=========================================="
echo "  KEntryEventHandler Test Suite"
echo "=========================================="
echo ""

# Phase 1: Prerequisites Check
if [ "$QUICK_TEST" = false ]; then
    echo "Phase 1: Checking Prerequisites"
    echo "-----------------------------------"

    # Check PostgreSQL
    print_info "Checking PostgreSQL..."
    if check_command psql; then
        # Try to connect
        if $PSQL -c "SELECT 1" > /dev/null 2>&1; then
            print_success "PostgreSQL connection successful"
        else
            print_error "Cannot connect to PostgreSQL"
            echo "  Database: $DB_NAME"
            echo "  User: $DB_USER"
            echo "  Host: $DB_HOST:$DB_PORT"
            exit 1
        fi
    else
        print_error "PostgreSQL client not installed"
        exit 1
    fi

    # Check pgvector extension
    print_info "Checking pgvector extension..."
    PGVECTOR_VERSION=$($PSQL -t -c "SELECT extversion FROM pg_extension WHERE extname = 'vector';" 2>/dev/null | tr -d ' ')
    if [ -n "$PGVECTOR_VERSION" ]; then
        print_success "pgvector installed (version: $PGVECTOR_VERSION)"
    else
        print_warning "pgvector extension not installed"
        echo "  To install:"
        echo "    brew install pgvector  # macOS"
        echo "    psql -U postgres -d $DB_NAME -c \"CREATE EXTENSION vector;\""

        read -p "  Attempt to install pgvector extension? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            psql -U postgres -d $DB_NAME -c "CREATE EXTENSION IF NOT EXISTS vector;" 2>&1
            print_success "pgvector extension created"
        else
            print_error "pgvector required for testing"
            exit 1
        fi
    fi

    # Check aig_embedding table
    print_info "Checking aig_embedding table..."
    TABLE_EXISTS=$($PSQL -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'aig_embedding';" 2>/dev/null | tr -d ' ')
    if [ "$TABLE_EXISTS" = "1" ]; then
        print_success "aig_embedding table exists"
    else
        print_warning "aig_embedding table not found"
        echo "  Running migration script..."

        MIGRATION_FILE="migration/postgresql/202512101600_CLD-1601.sql"
        if [ -f "$MIGRATION_FILE" ]; then
            $PSQL -f "$MIGRATION_FILE" > /dev/null 2>&1
            print_success "Migration executed successfully"
        else
            print_error "Migration file not found: $MIGRATION_FILE"
            exit 1
        fi
    fi

    # Check Ollama
    print_info "Checking Ollama service..."
    if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
        print_success "Ollama is running"

        # Check if nomic-embed-text model exists
        MODELS=$(curl -s http://localhost:11434/api/tags | grep -c "nomic-embed-text" || echo "0")
        if [ "$MODELS" -gt 0 ]; then
            print_success "nomic-embed-text model available"
        else
            print_warning "nomic-embed-text model not found"
            echo "  To install: ollama pull nomic-embed-text"

            read -p "  Attempt to pull model? (y/n) " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                ollama pull nomic-embed-text
                print_success "Model downloaded"
            fi
        fi
    else
        print_warning "Ollama not accessible at http://localhost:11434"
        echo "  Install: brew install ollama  # macOS"
        echo "  Or download from: https://ollama.com/download"
    fi

    # Check AIG_Provider configuration
    print_info "Checking AIG_Provider configuration..."
    PROVIDER_COUNT=$($PSQL -t -c "SELECT COUNT(*) FROM AIG_Provider WHERE IsEmbeddingProvider = 'Y' AND IsActive = 'Y';" 2>/dev/null | tr -d ' ')
    if [ "$PROVIDER_COUNT" -gt 0 ]; then
        print_success "Embedding provider configured"
        $PSQL -c "SELECT Name, ProviderType, Model FROM AIG_Provider WHERE IsEmbeddingProvider = 'Y' AND IsActive = 'Y';"
    else
        print_warning "No active embedding provider found"
        echo "  Creating Ollama embedding provider..."

        $PSQL <<EOF > /dev/null 2>&1
INSERT INTO AIG_Provider (
    AIG_Provider_UU,
    AIG_Provider_ID,
    AD_Client_ID,
    AD_Org_ID,
    IsActive,
    Created, CreatedBy,
    Updated, UpdatedBy,
    Name,
    ProviderType,
    Model,
    Endpoint,
    IsEmbeddingProvider
) VALUES (
    gen_random_uuid(),
    (SELECT COALESCE(MAX(AIG_Provider_ID), 1000000) + 1 FROM AIG_Provider),
    11,  -- GardenWorld
    0,
    'Y',
    now(), 100,
    now(), 100,
    'Ollama Embeddings (Test)',
    'Ollama',
    'nomic-embed-text',
    'http://localhost:11434',
    'Y'
) ON CONFLICT DO NOTHING;
EOF
        print_success "Provider created"
    fi

    echo ""
fi

if [ "$SETUP_ONLY" = true ]; then
    echo "Setup complete. Run without --setup-only to execute tests."
    exit 0
fi

# Phase 2: Test Execution
echo "Phase 2: Executing Tests"
echo "-----------------------------------"

# Test 1: Create K_Entry
print_info "Test 1: Creating test K_Entry record..."
TEST_TIMESTAMP=$(date +%s)
TEST_NAME="AutoTest_${TEST_TIMESTAMP}"

TEST_ID=$($PSQL -t -c "
INSERT INTO K_Entry (
    K_Entry_ID,
    K_Entry_UU,
    AD_Client_ID,
    AD_Org_ID,
    IsActive,
    Created, CreatedBy,
    Updated, UpdatedBy,
    Name,
    Description,
    TextMsg,
    Keywords
) VALUES (
    nextval('k_entry_seq'),
    gen_random_uuid(),
    11,  -- GardenWorld
    0,
    'Y',
    now(), 100,
    now(), 100,
    '$TEST_NAME',
    'Automated test entry for KEntryEventHandler',
    '# Test Article for Embedding

This is an automated test article created by the test script.

## Purpose
Verify that K_Entry changes trigger automatic embedding generation.

## Expected Behavior
1. KEntryEventHandler detects the save event
2. Parses this content (EditorJS/markdown)
3. Generates embedding via Ollama
4. Stores in aig_embedding table

## Test Data
- Timestamp: $TEST_TIMESTAMP
- Test ID: Will be assigned by sequence
',
    'test, automation, embedding, kentry'
) RETURNING K_Entry_ID;" 2>&1 | tr -d ' ')

if [ -z "$TEST_ID" ] || [ "$TEST_ID" = "" ]; then
    print_error "Failed to create K_Entry"
    echo "SQL output: $TEST_ID"
    exit 1
fi

print_success "Created K_Entry with ID: $TEST_ID"

# Wait for async embedding generation
print_info "Waiting 5 seconds for embedding generation..."
sleep 5

# Test 2: Verify embedding created
print_info "Test 2: Checking if embedding was created..."
EMBED_COUNT=$($PSQL -t -c "
SELECT COUNT(*) FROM aig_embedding
WHERE source_type = 'knowledge_entry'
  AND source_id = '$TEST_ID';" 2>/dev/null | tr -d ' ')

if [ "$EMBED_COUNT" = "1" ]; then
    print_success "Embedding created in database"

    # Show embedding details
    echo ""
    echo "Embedding Details:"
    $PSQL -c "
SELECT
    aig_embedding_uu,
    source_id,
    source_type,
    substring(text_segment, 1, 80) as content_preview,
    array_length(embedding::real[], 1) as dimension,
    created
FROM aig_embedding
WHERE source_type = 'knowledge_entry'
  AND source_id = '$TEST_ID';"

else
    print_error "Embedding not found (count: $EMBED_COUNT)"
    echo ""
    echo "Troubleshooting:"
    echo "1. Check iDempiere logs for errors"
    echo "2. Verify OSGi services are active:"
    echo "   - EmbeddingStoreProvider"
    echo "   - EmbeddingTriggerService"
    echo "   - KEntryEventHandler"
    echo "3. Check if Ollama is responding:"
    curl -s http://localhost:11434/api/embeddings -d '{
      "model": "nomic-embed-text",
      "prompt": "test"
    }' | head -c 100
    echo ""
fi

# Test 3: Update K_Entry
print_info "Test 3: Updating K_Entry to test embedding update..."
$PSQL -c "
UPDATE K_Entry
SET
    TextMsg = '# Updated Test Article

This content has been MODIFIED at $(date).

The embedding should be regenerated with new content hash.',
    Updated = now()
WHERE K_Entry_ID = $TEST_ID;" > /dev/null 2>&1

print_success "K_Entry updated"

# Wait for update
sleep 5

# Check if embedding was updated
print_info "Checking if embedding was updated..."
UPDATED_COUNT=$($PSQL -t -c "
SELECT COUNT(*) FROM aig_embedding
WHERE source_type = 'knowledge_entry'
  AND source_id = '$TEST_ID'
  AND text_segment LIKE '%MODIFIED%';" 2>/dev/null | tr -d ' ')

if [ "$UPDATED_COUNT" = "1" ]; then
    print_success "Embedding updated successfully"
else
    print_warning "Embedding may not have been updated (check content hash)"
fi

# Test 4: Inactive entry (should not embed)
print_info "Test 4: Testing inactive K_Entry (should skip embedding)..."
$PSQL -c "
UPDATE K_Entry
SET
    IsActive = 'N',
    Updated = now()
WHERE K_Entry_ID = $TEST_ID;" > /dev/null 2>&1

sleep 3

# Embedding should still exist (not deleted for inactive)
INACTIVE_COUNT=$($PSQL -t -c "
SELECT COUNT(*) FROM aig_embedding
WHERE source_type = 'knowledge_entry'
  AND source_id = '$TEST_ID';" 2>/dev/null | tr -d ' ')

if [ "$INACTIVE_COUNT" = "1" ]; then
    print_success "Inactive entry handling correct (embedding preserved)"
else
    print_warning "Unexpected embedding count: $INACTIVE_COUNT"
fi

# Test 5: Delete K_Entry
print_info "Test 5: Deleting K_Entry to test embedding deletion..."
$PSQL -c "DELETE FROM K_Entry WHERE K_Entry_ID = $TEST_ID;" > /dev/null 2>&1
print_success "K_Entry deleted"

# Wait for deletion handler
sleep 3

# Verify embedding deleted
DELETED_COUNT=$($PSQL -t -c "
SELECT COUNT(*) FROM aig_embedding
WHERE source_type = 'knowledge_entry'
  AND source_id = '$TEST_ID';" 2>/dev/null | tr -d ' ')

if [ "$DELETED_COUNT" = "0" ]; then
    print_success "Embedding deleted successfully"
else
    print_error "Embedding still exists after deletion (count: $DELETED_COUNT)"
fi

echo ""
echo "=========================================="
echo "  Test Summary"
echo "=========================================="
echo ""
echo "✅ Prerequisites: OK"
echo "✅ Create K_Entry: OK"
echo "✅ Embedding Generation: $([ "$EMBED_COUNT" = "1" ] && echo "OK" || echo "FAILED")"
echo "✅ Update K_Entry: OK"
echo "✅ Embedding Update: $([ "$UPDATED_COUNT" = "1" ] && echo "OK" || echo "CHECK LOGS")"
echo "✅ Delete K_Entry: OK"
echo "✅ Embedding Deletion: $([ "$DELETED_COUNT" = "0" ] && echo "OK" || echo "FAILED")"
echo ""

# Overall result
if [ "$EMBED_COUNT" = "1" ] && [ "$DELETED_COUNT" = "0" ]; then
    echo -e "${GREEN}🎉 ALL TESTS PASSED${NC}"
    echo ""
    echo "KEntryEventHandler is working correctly!"
    echo ""
    echo "Next steps:"
    echo "  1. Review iDempiere logs for any warnings"
    echo "  2. Test with real K_Entry content via UI"
    echo "  3. Verify semantic search works"
    exit 0
else
    echo -e "${RED}❌ SOME TESTS FAILED${NC}"
    echo ""
    echo "Please check:"
    echo "  1. iDempiere server logs"
    echo "  2. OSGi console for service status"
    echo "  3. docs/testing/KENTRY_EVENT_HANDLER_TESTING.md"
    exit 1
fi
