#!/bin/bash
data_dir="$1"
sb_dir="$2"
exit_code=0
cp $data_dir/positions/aaf/before/* $sb_dir/aaf-duplicates-test
docker compose run --rm k6 run file:///home/k6/src/it/scenarios/position/aaf-import-before.js
if [ $? -ne 0 ]; then
    exit_code=1
fi

cp $data_dir/positions/aaf/after/* $sb_dir/aaf-duplicates-test
docker compose run --rm k6 run file:///home/k6/src/it/scenarios/position/aaf-import-after.js
if [ $? -ne 0 ]; then
    exit_code=1
fi

exit $exit_code
