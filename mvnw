#!/bin/sh
set -e

BASE_DIR=$(cd "$(dirname "$0")" && pwd)
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
PROPS_FILE="$WRAPPER_DIR/maven-wrapper.properties"
JAR_FILE="$WRAPPER_DIR/maven-wrapper.jar"

if [ ! -f "$JAR_FILE" ]; then
  if [ ! -f "$PROPS_FILE" ]; then
    echo "Missing $PROPS_FILE" 1>&2
    exit 1
  fi
  WRAPPER_URL=$(grep -E '^wrapperUrl=' "$PROPS_FILE" | cut -d'=' -f2-)
  if [ -z "$WRAPPER_URL" ]; then
    echo "Missing wrapperUrl in $PROPS_FILE" 1>&2
    exit 1
  fi
  mkdir -p "$WRAPPER_DIR"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$WRAPPER_URL" -o "$JAR_FILE"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$WRAPPER_URL" -O "$JAR_FILE"
  else
    echo "Neither curl nor wget is available to download Maven wrapper" 1>&2
    exit 1
  fi
fi

exec java ${MAVEN_OPTS} -classpath "$JAR_FILE" -Dmaven.multiModuleProjectDirectory="$BASE_DIR" org.apache.maven.wrapper.MavenWrapperMain "$@"
