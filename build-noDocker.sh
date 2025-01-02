#!/bin/bash

if [ "$#" -lt 1 ]; then
  echo "Usage: $0 <clean|install|buildFrontend|buildBackend|watch>"
  echo "Example: $0 clean install"
  exit 1
fi

./build.sh --no-docker $@
if [ ! $? -eq 0 ]; then
  exit 1
fi
