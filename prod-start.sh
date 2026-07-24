#!/bin/sh

java \
  -XX:+UseG1GC \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -Dspring.profiles.active=prod \
  -jar ams-0.0.1-SNAPSHOT.jar
