#!/bin/bash
#
# Test POST /api/cars with Clerk JWT
#
# Usage:
#   ./scripts/clerk/3-create-car.sh [staging|production]
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
echo -e "${BLUE}  Test: POST /api/cars${NC}"
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
    exit 1
  fi
fi

echo ""

# Create car data
CAR_DATA='{
  "make": "Tesla",
  "model": "Model 3",
  "year": 2023,
  "name": "Test Tesla",
  "distanceUnit": "MILES",
  "fuelUnit": "GALLONS"
}'

echo -e "${BLUE}➤${NC} Creating car..."
echo -e "${BLUE}Request body:${NC}"
echo "$CAR_DATA" | jq .
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" \
  -X POST "$API_URL/api/cars" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d "$CAR_DATA")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" == "201" ] || [ "$HTTP_CODE" == "200" ]; then
  echo -e "${GREEN}✓${NC} Car created successfully (HTTP $HTTP_CODE)"
  echo ""
  echo -e "${BLUE}Response:${NC}"
  echo "$BODY" | jq .

  CAR_ID=$(echo "$BODY" | jq -r '.id')
  echo ""
  echo -e "${GREEN}✓${NC} Car ID: $CAR_ID"
else
  echo -e "${RED}✗${NC} Failed to create car (HTTP $HTTP_CODE)"
  echo ""
  echo -e "${YELLOW}Response:${NC}"
  echo "$BODY" | jq .
fi
echo ""
