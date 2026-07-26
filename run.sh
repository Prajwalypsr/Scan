#!/usr/bin/env bash
# DiskInsight - compile and run
set -e
cd "$(dirname "$0")"

# put mysql-connector-j-*.jar in lib/ to enable the database (optional)
CP="out"
if compgen -G "lib/*.jar" > /dev/null; then
  CP="out:$(echo lib/*.jar | tr ' ' ':')"
fi

echo "Compiling..."
mkdir -p out
javac -d out src/diskinsight/*.java

echo "Starting DiskInsight..."
java -cp "$CP" diskinsight.DiskInsightApp
