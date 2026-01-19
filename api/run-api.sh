#!/bin/bash
set -e

[ -f src/main/resources/app.db ] || touch src/main/resources/app.db

./gradlew bootRun
