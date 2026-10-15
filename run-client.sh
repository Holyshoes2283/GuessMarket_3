#!/bin/bash

echo "======================================================="
echo "       GuessMarket JavaFX Client Launcher (macOS)      "
echo "======================================================="
echo ""
echo "NOTE: Ensure Tomcat 10 is running locally on port 8080."
echo "The GuessMarket.war must be fully deployed."
echo "======================================================="
echo ""

# Resolving artifact path relative to the project root
JAR_FILE="ui/target/ui-1.0-SNAPSHOT.jar"

# Environment Validation
if [ -f "$JAR_FILE" ]; then
    echo "Artifact located. Launching $JAR_FILE..."
    java -jar "$JAR_FILE"
else
    echo "ENVIRONMENT ERROR: $JAR_FILE not found."
    echo "Execute 'mvn clean install' from the root directory to generate the artifact."
    exit 1
fi
