#!/bin/bash

MVN_OPTS="-Duser.home=/var/maven"

case `uname -s` in
  MINGW*)
    USER_UID=1000
    GROUP_GID=1000
    ;;
  *)
    if [ -z ${USER_UID:+x} ]
    then
      USER_UID=`id -u`
      GROUP_GID=`id -g`
    fi
esac

init() {
  me=`id -u`:`id -g`
  echo "DEFAULT_DOCKER_USER=$me" > .env
}

clean () {
  docker-compose run --rm -T maven mvn $MVN_OPTS clean
}

install () {
  docker-compose run --rm -T maven mvn $MVN_OPTS install -DskipTests
}

test () {
  docker-compose run --rm -T maven mvn $MVN_OPTS test
}

publish() {
  version=`docker-compose run --rm -T maven mvn $MVN_OPTS help:evaluate -Dexpression=project.version -q -DforceStdout`
  level=`echo $version | cut -d'-' -f3`
  case "$level" in
    *SNAPSHOT) export nexusRepository='snapshots' ;;
    *)         export nexusRepository='releases' ;;
  esac

  docker-compose run --rm -T maven mvn $MVN_OPTS -DrepositoryId=ode-$nexusRepository -DskipTests --settings /var/maven/.m2/settings.xml deploy
}

if [ ! -f .env ]; then
  init
fi

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
    test)
      test
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

