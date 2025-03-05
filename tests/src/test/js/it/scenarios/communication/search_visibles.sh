#!/bin/bash
data_dir="$1"
sb_dir="$2"
current_dir="$(pwd)"
exit_code=0
# set DEBUG_SUSPEND=n in docker-compose.yml
sed -i 's/DEBUG_SUSPEND=y/DEBUG_SUSPEND=n/' $sb_dir/docker-compose.yml

# set "visibles-search-type": "legacy" in ent-core.json
sed -i 's/"visibles-search-type.*"/"visibles-search-type": "legacy"/' $sb_dir/ent-core.json
# restart vertx container
cd $sb_dir
docker compose up -d --force-recreate vertx
# wait for vertx to restart
sleep 15
# run k6 test for _search-visibles-legacy.ts
cd $current_dir
docker compose run --rm k6 run --compatibility-mode=experimental_enhanced file:///home/k6/src/it/scenarios/communication/_search-visibles-legacy.ts
if [ $? -ne 0 ]; then
    exit_code=1
fi

# set "visibles-search-type": "light" in ent-core.json
sed -i 's/"visibles-search-type.*"/"visibles-search-type": "light"/' $sb_dir/ent-core.json
# restart vertx container
cd $sb_dir
docker compose up -d --force-recreate vertx
# wait for vertx to restart
sleep 15
# run k6 test for _search-visibles-light.ts
cd $current_dir
docker compose run --rm k6 run --compatibility-mode=experimental_enhanced file:///home/k6/src/it/scenarios/communication/_search-visibles-light.ts
if [ $? -ne 0 ]; then
    exit_code=1
fi

exit $exit_code
