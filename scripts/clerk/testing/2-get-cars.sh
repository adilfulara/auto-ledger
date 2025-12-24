#!/bin/bash
#
# Test GET /api/cars with Clerk JWT
#
# Usage:
#   ./scripts/clerk/2-get-all-cars.sh [staging|production]
#
# Prerequisites:
#   - Run ./scripts/clerk/1-issue-jwt.sh first
#   - Or set JWT environment variable
#

set -euo pipefail

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

ENVIRONMENT="${1:-staging}"

# Determine URL
if [ "$ENVIRONMENT" == "production" ]; then
  API_URL="https://auto-ledger-prod.fly.dev"
else
  API_URL="https://auto-ledger-staging.fly.dev"
fi

echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  Test: GET /api/cars${NC}"
echo -e "${BLUE}  Environment: $ENVIRONMENT${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo ""

# Check for JWT
if [ -z "${JWT:-}" ]; then
  if [ -f /tmp/clerk_jwt.txt ]; then
    JWT=$(cat /tmp/clerk_jwt.txt)
    echo -e "${BLUE}ℹ${NC} Using JWT from /tmp/clerk_jwt.txt"
  else
    echo -e "${YELLOW}⚠${NC} No JWT found"
    echo ""
    echo "Run this first:"
    echo "  ./scripts/clerk/1-issue-jwt.sh"
    echo ""
    echo "Then export the JWT:"
    echo "  export JWT=\$(cat /tmp/clerk_jwt.txt)"
    exit 1
  fi
fi

echo ""

# Test 1: Without JWT (should fail)
echo -e "${BLUE}➤${NC} Test 1: Request WITHOUT JWT (should be 401)"
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_URL/api/cars")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" == "401" ]; then
  echo -e "${GREEN}✓${NC} Correctly rejected (HTTP $HTTP_CODE)"
else
  echo -e "${RED}✗${NC} Expected 401, got HTTP $HTTP_CODE"
fi
echo ""

# Test 2: With JWT (should succeed)
echo -e "${BLUE}➤${NC} Test 2: Request WITH JWT (should be 200)"
RESPONSE=$(curl -s -w "\n%{http_code}" \
  -H "Authorization: Bearer $JWT" \
  "$API_URL/api/cars")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" == "200" ]; then
  echo -e "${GREEN}✓${NC} Authentication successful (HTTP $HTTP_CODE)"
  echo ""
  echo -e "${BLUE}Response:${NC}"
  echo "$BODY" | jq .

  COUNT=$(echo "$BODY" | jq 'length')
  if [ "$COUNT" == "0" ]; then
    echo ""
    echo -e "${BLUE}ℹ${NC} No cars found (expected for new users)"
  else
    echo ""
    echo -e "${GREEN}✓${NC} Found $COUNT car(s)"
  fi
else
  echo -e "${RED}✗${NC} Authentication failed (HTTP $HTTP_CODE)"
  echo ""
  echo -e "${YELLOW}Response:${NC}"
  echo "$BODY" | jq .
  echo ""
  echo -e "${YELLOW}Troubleshooting:${NC}"
  echo "  • JWT might be expired (valid for 10 minutes)"
  echo "  • Run: ./scripts/clerk/1-issue-jwt.sh to get a fresh token"
  echo "  • Check backend logs: fly logs -a auto-ledger-$ENVIRONMENT"
fi
echo ""
