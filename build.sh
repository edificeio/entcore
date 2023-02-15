#!/bin/bash

if [ ! -e node_modules ]
then
  mkdir node_modules
fi

case `uname -s` in
  MINGW* | Darwin*)
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
MODULE=""
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

if [ "$MODULE" = "" ]; then
    GRADLE_OPTION=""
    NODE_OPTION=""
else
  GRADLE_OPTION=":$MODULE:"
  NODE_OPTION="--module $MODULE"
fi

#try jenkins branch name => then local git branch name => then jenkins params
echo "[buildNode] Get branch name from jenkins env..."
BRANCH_NAME=`echo $GIT_BRANCH | sed -e "s|origin/||g"`
if [ "$BRANCH_NAME" = "" ]; then
  echo "[buildNode] Get branch name from git..."
  BRANCH_NAME=`git branch | sed -n -e "s/^\* \(.*\)/\1/p"`
fi
if [ ! -z "$FRONT_TAG" ]; then
  echo "[buildNode] Get tag name from jenkins param... $FRONT_TAG"
  BRANCH_NAME="$FRONT_TAG"
fi
if [ "$BRANCH_NAME" = "" ]; then
  echo "[buildNode] Branch name should not be empty!"
  exit -1
fi

echo "======================"
echo "BRANCH_NAME = $BRANCH_NAME"
echo "======================"

clean () {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle clean
}

buildNode () {
  if [ "$MODULE" = "" ] || [ ! "$MODULE" = "admin" ]; then
    #try jenkins branch name => then local git branch name => then jenkins params
    echo "[buildNode] Get branch name from jenkins env..."
    BRANCH_NAME=`echo $GIT_BRANCH | sed -e "s|origin/||g"`
    if [ "$BRANCH_NAME" = "" ]; then
      echo "[buildNode] Get branch name from git..."
      BRANCH_NAME=`git branch | sed -n -e "s/^\* \(.*\)/\1/p"`
    fi
    if [ ! -z "$FRONT_TAG" ]; then
      echo "[buildNode] Get tag name from jenkins param... $FRONT_TAG"
      BRANCH_NAME="$FRONT_TAG"
    fi
    if [ "$BRANCH_NAME" = "" ]; then
      echo "[buildNode] Branch name should not be empty!"
      exit -1
    fi

    if [ "$BRANCH_NAME" = 'master' ] || [ "$BRANCH_NAME" = 'fix' ]  || [ "$BRANCH_NAME" = 'release' ]; then
        echo "[buildNode] Use entcore version from package.json ($BRANCH_NAME)"
        case `uname -s` in
          MINGW*)
            docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install --no-bin-links --legacy-peer-deps && npm update entcore && node_modules/gulp/bin/gulp.js build $NODE_OPTION"
            ;;
          *)
            docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install --legacy-peer-deps && npm update entcore && node_modules/gulp/bin/gulp.js build $NODE_OPTION --springboard=/home/node/$SPRINGBOARD"
        esac
    else
        echo "[buildNode] Use entcore tag $BRANCH_NAME"
        docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm rm --no-save entcore ode-ts-client ode-ngjs-front && npm install --no-save entcore@$BRANCH_NAME ode-ts-client@$BRANCH_NAME ode-ngjs-front@$BRANCH_NAME"
        case `uname -s` in
          MINGW*)
            docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install --no-bin-links --legacy-peer-deps && node_modules/gulp/bin/gulp.js build $NODE_OPTION"
            ;;
          *)
            docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install --legacy-peer-deps && node_modules/gulp/bin/gulp.js build $NODE_OPTION --springboard=/home/node/$SPRINGBOARD"
        esac
    fi
  fi
}

buildAdminNode() {
  if [ "$MODULE" = "" ] || [ "$MODULE" = "admin" ]; then
    case `uname -s` in
      MINGW*)
        docker-compose run --rm -u "$USER_UID:$GROUP_GID" node16 sh -c "npm install --no-bin-links && npm rm --no-save ngx-ode-core ngx-ode-sijil ngx-ode-ui && npm install --no-save ngx-ode-core@$BRANCH_NAME ngx-ode-sijil@$BRANCH_NAME ngx-ode-ui@$BRANCH_NAME && npm run build-docker-prod"
        ;;
      *)
        docker-compose run --rm -u "$USER_UID:$GROUP_GID" node16 sh -c "npm install && npm rm --no-save ngx-ode-core ngx-ode-sijil ngx-ode-ui && npm install --no-save ngx-ode-core@$BRANCH_NAME ngx-ode-sijil@$BRANCH_NAME ngx-ode-ui@$BRANCH_NAME && npm run build-docker-prod"
    esac
  fi
}

buildGradle () {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle "$GRADLE_OPTION"shadowJar "$GRADLE_OPTION"install "$GRADLE_OPTION"publishToMavenLocal
}

testGradle () {
  ./gradlew "$GRADLE_OPTION"test
}

localDep () {
  for dep in ode-ts-client ode-ngjs-front ; do
    if [ -e $PWD/../$dep ]; then
      rm -rf $dep.tar $dep.tar.gz
      mkdir $dep.tar && mkdir $dep.tar/dist \
        && cp -R $PWD/../$dep/dist $PWD/../$dep/package.json $dep.tar
      tar cfzh $dep.tar.gz $dep.tar
      docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install --no-save $dep.tar.gz"
      rm -rf $dep.tar $dep.tar.gz
    fi
  done
}

watch () {
  docker-compose run --rm \
    -u "$USER_UID:$GROUP_GID" \
    -v $PWD/../$SPRINGBOARD:/home/node/$SPRINGBOARD \
    node sh -c "npx gulp watch-$MODULE $NODE_OPTION --springboard=../$SPRINGBOARD 2>/dev/null"
}

# ex: ./build.sh -m=workspace -s=paris watch

ngWatch () {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" node16 sh -c "npm run start"
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
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle "$GRADLE_OPTION"publish
}

for param in "$@"
do
  case $param in
    clean)
      clean
      ;;
    buildAdminNode)
      buildAdminNode
      ;;
    buildNode)
      buildNode
      ;;
    buildGradle)
      buildGradle
      ;;
    install)
      buildNode && buildAdminNode && buildGradle
      ;;
    localDep)
      localDep
      ;;
    watch)
      watch
      ;;
    ngWatch)
      ngWatch
      ;;
    test)
      testGradle
      ;;
    infra)
      infra
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