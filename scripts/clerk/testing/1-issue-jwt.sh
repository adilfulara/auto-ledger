#!/bin/bash
#
# Issue a fresh JWT token from Clerk for testing
#
# Usage:
#   ./scripts/clerk/1-issue-jwt.sh
#
# Prerequisites:
#   - Clerk account with test user created
#   - CLERK_SECRET_KEY environment variable set
#   - CLERK_USER_ID environment variable set
#

set -euo pipefail

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  Clerk JWT Token Generator${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo ""

# Check for required environment variables
if [ -z "${CLERK_SECRET_KEY:-}" ]; then
  echo -e "${YELLOW}⚠${NC} CLERK_SECRET_KEY not set"
  echo ""
  echo "Set it with:"
  echo "  export CLERK_SECRET_KEY='sk_test_...'"
  echo ""
  echo "Get it from: Clerk Dashboard → Configure → API Keys → Secret keys"
  exit 1
fi

if [ -z "${CLERK_USER_ID:-}" ]; then
  echo -e "${YELLOW}⚠${NC} CLERK_USER_ID not set"
  echo ""
  echo "Set it with:"
  echo "  export CLERK_USER_ID='user_...'"
  echo ""
  echo "Get it from: Clerk Dashboard → Users → Click on user → Copy ID"
  exit 1
fi

echo -e "${BLUE}ℹ${NC} Secret Key: ${CLERK_SECRET_KEY:0:20}..."
echo -e "${BLUE}ℹ${NC} User ID: $CLERK_USER_ID"
echo ""

# Step 1: Create session
echo -e "${BLUE}➤${NC} Creating session..."
SESSION_RESPONSE=$(curl -s -X POST "https://api.clerk.com/v1/sessions" \
  -H "Authorization: Bearer $CLERK_SECRET_KEY" \
  -H "Content-Type: application/json" \
  -d "{\"user_id\": \"$CLERK_USER_ID\"}")

SESSION_ID=$(echo "$SESSION_RESPONSE" | jq -r '.id')

if [ "$SESSION_ID" == "null" ]; then
  echo -e "${YELLOW}✗${NC} Failed to create session"
  echo "$SESSION_RESPONSE" | jq .
  exit 1
fi

echo -e "${GREEN}✓${NC} Session created: $SESSION_ID"
echo ""

# Step 2: Generate JWT
echo -e "${BLUE}➤${NC} Generating JWT with 'testing-template'..."
JWT_RESPONSE=$(curl -s -X POST "https://api.clerk.com/v1/sessions/$SESSION_ID/tokens/testing-template" \
  -H "Authorization: Bearer $CLERK_SECRET_KEY" \
  -H "Content-Type: application/json")

JWT=$(echo "$JWT_RESPONSE" | jq -r '.jwt')

if [ "$JWT" == "null" ]; then
  echo -e "${YELLOW}✗${NC} Failed to generate JWT"
  echo "$JWT_RESPONSE" | jq .
  exit 1
fi

echo -e "${GREEN}✓${NC} JWT generated successfully"
echo ""

# Step 3: Save JWT for use in other scripts (do this first!)
echo "$JWT" > /tmp/clerk_jwt.txt
echo -e "${GREEN}✓${NC} JWT saved to: /tmp/clerk_jwt.txt"
echo ""

# Step 4: Decode and display JWT claims
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  JWT Claims:${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo "$JWT" | cut -d'.' -f2 | base64 --decode 2>/dev/null | jq . || echo "(Could not decode JWT - but it's saved!)"
echo ""

# Step 5: Usage instructions
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  Usage:${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo ""
echo "To use this JWT in other scripts:"
echo ""
echo -e "${GREEN}  export JWT=\$(cat /tmp/clerk_jwt.txt)${NC}"
echo ""
echo "Or run the test scripts:"
echo ""
echo -e "${GREEN}  ./scripts/clerk/2-get-all-cars.sh${NC}"
echo -e "${GREEN}  ./scripts/clerk/3-create-car.sh${NC}"
echo ""
