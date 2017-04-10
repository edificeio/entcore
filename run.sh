#!/bin/bash
docker-compose up -d neo4j > /dev/null &
sleep 10
vertx runMod org.entcore~infra~1.27-SNAPSHOT > /dev/null &
