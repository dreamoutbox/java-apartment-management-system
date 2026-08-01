#!/bin/sh

# Free port 8080 if held by orphan background process
# fuser -k 8080/tcp >/dev/null 2>&1 || true

./mvnw spring-boot:run

# App URL: http://localhost:8080

# Adminer: http://localhost:9001

# H2 Console: http://localhost:8080/h2-console 
# (JDBC URL: jdbc:h2:mem:amsdb, Username: sa)
# ==================================================
# for SQLite
# Field             Value
# Driver Class	    org.sqlite.JDBC
# JDBC URL	        jdbc:sqlite:./data/ams.db
# User Name	        (leave empty)
# Password	        (leave empty)
