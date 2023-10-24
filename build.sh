#!/bin/bash

MVN_OPTS="-Duser.home=/var/maven"

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

init() {
  me=`id -u`:`id -g`
  echo "DEFAULT_DOCKER_USER=$me" > .env
}

clean () {
  docker compose run --rm maven mvn $MVN_OPTS clean
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

install () {
  docker compose run --rm maven mvn $MVN_OPTS install -DskipTests
}

test () {
  if [ -z "$JAVA_8_HOME" ]
  then
    mvn test
  else
    JAVA_HOME=$JAVA_8_HOME mvn test
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

publish() {
  version=`docker-compose run --rm maven mvn $MVN_OPTS help:evaluate -Dexpression=project.version -q -DforceStdout`
  level=`echo $version | cut -d'-' -f3`
  case "$level" in
    *SNAPSHOT) export nexusRepository='snapshots' ;;
    *)         export nexusRepository='releases' ;;
  esac

  docker compose run --rm  maven mvn -DrepositoryId=ode-$nexusRepository -DskipTests --settings /var/maven/.m2/settings.xml deploy
}

check_prefix_sh_file() {
    dir_path=$1      # Directory path
    search_str=$2    # String to check
    # Loop over each .sh file in the directory
    for file in "$dir_path"/*.sh; do
        if [ -f "$file" ]; then
            # Get the file name without the extension
            base_name=$(basename "$file" .sh)

            # Check if the file name is a prefix of the search string
            if [[ "$search_str" == "$base_name"* ]]; then
                return 0  # Found a match, return 0 (success)
            fi
        fi
    done

    return 1  # No match found, return 1 (failure)
}

itTests() {
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  cd $script_dir/tests/src/test/js/it/scenarios
  exit_code=0
  js_files=($(find . -type f -name '*.js' ! -name '_*'))
  for it_file in "${js_files[@]}"; do
    short_file_name=$(basename -s .js $it_file)
    file_dir=$(dirname $it_file)
    check_prefix_sh_file "$file_dir" "$short_file_name"
    if [ $? -eq 1 ]; then
      echo executing $it_file
      docker compose run --rm -T k6 run file:///home/k6/src/it/scenarios/$it_file
      if [ $? -ne 0 ]; then
          exit_code=1
          echo "Error while executing : $it_file"
      fi
    fi
  done
  sh_files=($(find . -type f -name '*.sh'))
  for sh_file in "${sh_files[@]}"; do
    echo executing $sh_file
    "$sh_file" "$script_dir/tests/src/test/resources/data" "$script_dir/../$SPRINGBOARD"
    if [ $? -ne 0 ]; then
        exit_code=1
        echo "Error while executing : $sh_file"
    fi
  done
  cd -

  echo "|-------------------------|"
  [ $exit_code -ne 0 ] && echo "|---- itTests  FAILED ----|" || echo "|--- itTests SUCCEEDED ---|"
  echo "|-------------------------|"
  exit $exit_code
}

for param in "$@"
do
  case $param in
    '--no-user')
      ;;
    init)
      init
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
    install)
      buildNode && buildAdminNode && install
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
      test
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
