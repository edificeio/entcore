#!/bin/bash
docker-compose up -d neo4j > /dev/null &
sleep 10
vertx runMod org.entcore~infra~1.28.11 > /dev/null &
