#!/usr/bin/env bash
set -ev
./gradlew --no-daemon --version
./mvnw --version
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --continue :build "$@"
./gradlew --no-daemon -Dmaven.repo.local=dist/m2 --continue :gradle-plugins:build
./mvnw -Dmaven.repo.local=dist/m2  -Dhttp.connectionTimeout=100000 -Dhttp.retryCount=5 --batch-mode --no-transfer-progress install
