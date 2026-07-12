#!/usr/bin/env sh

DIRNAME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CLASSPATH="$DIRNAME/gradle/wrapper/gradle-wrapper.jar"
JAVA_CMD="java"
exec "$JAVA_CMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
