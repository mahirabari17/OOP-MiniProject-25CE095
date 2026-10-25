#!/bin/bash
# Compiles MiniBank and builds the runnable JAR (needs only a JDK 17+).
set -e
rm -rf out && mkdir -p out
javac -d out $(find src/main -name "*.java")
jar --create --file minibank.jar --main-class app.MiniBank -C out .
echo "Built minibank.jar  ->  run with: java -jar minibank.jar"
