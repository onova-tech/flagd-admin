#!/bin/bash
set -e

export VITE_API_BASE_URL="${VITE_API_BASE_URL:-http://localhost:9090}"

npm run dev
