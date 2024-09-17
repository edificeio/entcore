#!/bin/bash
tests=(
    "docker compose run --rm k6 run file:///home/k6/src/it/scenarios/position/crud.js"
    #"docker compose run --rm k6 run file:///home/k6/src/it/scenarios/position/attribute-position.js"
)
exit_code=0
for test in "${tests[@]}"; do
    # Run the command
    $test
    
    # Check the exit status of the command
    if [ $? -ne 0 ]; then
        exit_code=1
    fi
done
if [ $exit_code == 1 ]; then
    echo "Positions : some tests failed"
fi
exit $exit_code