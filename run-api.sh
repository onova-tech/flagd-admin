#!/bin/bash
set -e

cd "$(dirname "$0")/api"

./gradlew bootRun
