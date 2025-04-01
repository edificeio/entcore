#!/bin/bash

set -e

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

clean() {
  cd $SCRIPT_DIR/ts
  pnpm clean
  rm -Rf src/nats/*
  #rm -Rf src/rest/*
}

install() {
  cd $SCRIPT_DIR/ts
  pnpm build
}

init() {
  cd $SCRIPT_DIR/ts
  pnpm i
}


for param in "$@"
do
  case $param in
    init)
      init
      ;;
    clean)
      clean
      ;;
    install)
      install
      ;;
    publish)
      publish
      ;;
    *)
      echo "Invalid argument : $param"
  esac
  if [ ! $? -eq 0 ]; then
    exit 1
  fi
done

