#!/bin/bash

if [ ! -e node_modules ]
then
  mkdir node_modules
fi

case `uname -s` in
  MINGW*)
    USER_UID=1000
    GROUP_UID=1000
    ;;
  *)
    if [ -z ${USER_UID:+x} ]
    then
      USER_UID=`id -u`
      GROUP_GID=`id -g`
    fi
esac

# options
SPRINGBOARD="recette"
MODULE="conversation"
for i in "$@"
do
case $i in
    -s=*|--springboard=*)
    SPRINGBOARD="${i#*=}"
    shift
    ;;
    -m=*|--module=*)
    MODULE="${i#*=}"
    shift
    ;;
    *)
    ;;
esac
done

clean () {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle clean
}

buildNode () {
  case `uname -s` in
    MINGW*)
      docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install --no-bin-links && npm update entcore && node_modules/gulp/bin/gulp.js build" 
      ;;
    *)
      docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install && npm update entcore && node_modules/gulp/bin/gulp.js build --springboard=/home/node/$SPRINGBOARD"
  esac
}

buildGradle () {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle shadowJar install publishToMavenLocal
}

watch () {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "node_modules/gulp/bin/gulp.js watch-$MODULE --springboard=/home/node/$SPRINGBOARD"
}

infra () {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install /home/node/infra-front"
}

publish () {
  if [ -e "?/.gradle" ] && [ ! -e "?/.gradle/gradle.properties" ]
  then
    echo "odeUsername=$NEXUS_ODE_USERNAME" > "?/.gradle/gradle.properties"
    echo "odePassword=$NEXUS_ODE_PASSWORD" >> "?/.gradle/gradle.properties"
    echo "sonatypeUsername=$NEXUS_SONATYPE_USERNAME" >> "?/.gradle/gradle.properties"
    echo "sonatypePassword=$NEXUS_SONATYPE_PASSWORD" >> "?/.gradle/gradle.properties"
  fi
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle publish
}

# Admin v2 tasks
adminV2GradleClean () {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle :admin:clean
}

adminV2GradleInstall () {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle :admin:shadowJar :admin:install :admin:publishToMavenLocal
}

adminV2NodeClean () {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install && npm update entcore && node_modules/gulp/bin/gulp.js adminV2-clean"
}

adminV2NodeBuildDev () {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install && npm update entcore && node_modules/gulp/bin/gulp.js adminV2-build-dev"
}

adminV2NodeBuildProd () {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install && npm update entcore && node_modules/gulp/bin/gulp.js adminV2-build"
}

adminV2NodeDevServer () {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install && npm update entcore && node_modules/gulp/bin/gulp.js adminV2-dev-server"
}

adminV2NodeWatch () {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "node_modules/gulp/bin/gulp.js adminV2-watch"
}
# End of AdminV2 tasks

for param in "$@"
do
  case $param in
    clean)
      clean
      ;;
    buildNode)
      buildNode
      ;;
    buildGradle)
      buildGradle
      ;;
    install)
      buildNode && buildGradle
      ;;
    watch)
      watch
      ;;
    infra)
      infra
      ;;
    publish)
      publish
      ;;
    adminV2GradleClean)
      adminV2GradleClean
      ;;
    adminV2GradleInstall)
      adminV2GradleInstall
      ;;
    adminV2NodeClean)
      adminV2NodeClean
      ;;
    adminV2NodeBuildDev)
      adminV2NodeBuildDev
      ;;
    adminV2NodeBuildProd)
      adminV2NodeBuildProd
      ;;
    adminV2NodeDevServer)
      adminV2NodeDevServer
      ;;
    adminV2NodeWatch)
      adminV2NodeWatch
      ;;
    *)
      echo "Invalid argument : $param"
  esac
  if [ ! $? -eq 0 ]; then
    exit 1
  fi
done

