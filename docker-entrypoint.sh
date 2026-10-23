#!/bin/sh
# Hosts like Render hand out the database as separate parts; build the JDBC URL Spring needs from them.
if [ -z "$SPRING_DATASOURCE_URL" ] && [ -n "$DB_HOST" ]; then
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT:-5432}/${DB_NAME}"
fi
exec java $JAVA_OPTS -jar app.jar
