#!/bin/bash

if [ ! -e node_modules ]
then
  mkdir node_modules
fi


if [[ "$*" == *"--no-user"* ]]
then
  USER_OPTION=""
else
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
  USER_OPTION="-u $USER_UID:$GROUP_GID"
fi

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
  docker compose run --rm $USER_OPTION gradle gradle clean
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
            docker compose run --rm $USER_OPTION node sh -c "npm install --no-bin-links --legacy-peer-deps && npm update --legacy-peer-deps entcore && node_modules/gulp/bin/gulp.js build $NODE_OPTION"
            ;;
          *)
            docker compose run --rm $USER_OPTION node sh -c "npm install --legacy-peer-deps && npm update --legacy-peer-deps entcore && node_modules/gulp/bin/gulp.js build $NODE_OPTION --springboard=/home/node/$SPRINGBOARD"
        esac
    else
        echo "[buildNode] Use entcore tag $BRANCH_NAME"
        docker compose run --rm $USER_OPTION node sh -c "npm rm --no-save entcore ode-ts-client ode-ngjs-front && npm install --no-save entcore@$BRANCH_NAME ode-ts-client@$BRANCH_NAME ode-ngjs-front@$BRANCH_NAME"
        case `uname -s` in
          MINGW*)
            docker compose run --rm $USER_OPTION node sh -c "npm install --no-bin-links --legacy-peer-deps && node_modules/gulp/bin/gulp.js build $NODE_OPTION"
            ;;
          *)
            docker compose run --rm $USER_OPTION node sh -c "npm install --legacy-peer-deps && node_modules/gulp/bin/gulp.js build $NODE_OPTION --springboard=/home/node/$SPRINGBOARD"
        esac
    fi
  fi
}

buildAdminNode() {
  if [ "$MODULE" = "" ] || [ "$MODULE" = "admin" ]; then
    case `uname -s` in
      MINGW*)
        docker compose run --rm $USER_OPTION node16 sh -c "npm install --no-bin-links && npm rm --no-save ngx-ode-core ngx-ode-sijil ngx-ode-ui && npm install --no-save ngx-ode-core@$BRANCH_NAME ngx-ode-sijil@$BRANCH_NAME ngx-ode-ui@$BRANCH_NAME && npm run build-docker-prod"
        ;;
      *)
        docker compose run --rm $USER_OPTION node16 sh -c "npm install && npm rm --no-save ngx-ode-core ngx-ode-sijil ngx-ode-ui && npm install --no-save ngx-ode-core@$BRANCH_NAME ngx-ode-sijil@$BRANCH_NAME ngx-ode-ui@$BRANCH_NAME && npm run build-docker-prod"
    esac
  fi
}

buildGradle () {
  docker compose run --rm $USER_OPTION gradle bash -c "git config --add safe.directory /home/gradle/project && gradle ${GRADLE_OPTION}shadowJar ${GRADLE_OPTION}install"
}

testGradle () {
  if [ -z "$JAVA_8_HOME" ]
  then
    ./gradlew "$GRADLE_OPTION"test
  else
    JAVA_HOME=$JAVA_8_HOME ./gradlew "$GRADLE_OPTION"test
  fi
}

localDep () {
  for dep in ode-ts-client ode-ngjs-front ; do
    if [ -e $PWD/../$dep ]; then
      rm -rf $dep.tar $dep.tar.gz
      mkdir $dep.tar && mkdir $dep.tar/dist \
        && cp -R $PWD/../$dep/dist $PWD/../$dep/package.json $dep.tar
      tar cfzh $dep.tar.gz $dep.tar
      docker compose run --rm $USER_OPTION node sh -c "npm install --no-save $dep.tar.gz"
      rm -rf $dep.tar $dep.tar.gz
    fi
  done
}

watch () {
  docker compose run --rm \
    $USER_OPTION \
    -v $PWD/../$SPRINGBOARD:/home/node/$SPRINGBOARD \
    node sh -c "npx gulp watch-$MODULE $NODE_OPTION --springboard=../$SPRINGBOARD 2>/dev/null"
}

# ex: ./build.sh -m=workspace -s=paris watch

ngWatch () {
  docker compose run --rm $USER_OPTION --publish 4200:4200 node16 sh -c "npm run start"
}

infra () {
  docker compose run --rm $USER_OPTION node sh -c "npm install /home/node/infra-front"
}

publish () {
  if [ -e "?/.gradle" ] && [ ! -e "?/.gradle/gradle.properties" ]
  then
    echo "odeUsername=$NEXUS_ODE_USERNAME" > "?/.gradle/gradle.properties"
    echo "odePassword=$NEXUS_ODE_PASSWORD" >> "?/.gradle/gradle.properties"
    echo "sonatypeUsername=$NEXUS_SONATYPE_USERNAME" >> "?/.gradle/gradle.properties"
    echo "sonatypePassword=$NEXUS_SONATYPE_PASSWORD" >> "?/.gradle/gradle.properties"
  fi
  docker compose run --rm $USER_OPTION gradle gradle "$GRADLE_OPTION"publish
}

itTests() {
  all_successful=true
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  for script in $(find "$script_dir/tests/src/test/js/it" -type f -name "run.sh"); do
    # Execute the script
    bash "$script"
    
    # Check the exit status of the script
    if [ $? -ne 0 ]; then
        all_successful=false  # If any script fails, exit with status 1
    fi
  done
  if [ "$all_successful" = true ]; then
    exit 0
  else
    exit 1
  fi
}

for param in "$@"
do
  case $param in
    '--no-user')
      ;;
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
    itTests)
      itTests
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
