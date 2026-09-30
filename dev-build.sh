#!/bin/sh

# Maven
./mvnw clean package -DskipTests=false

# or Gradle
# ./gradlew clean bootJar
