#!/bin/bash

if [ ! -e node_modules ]
then
  mkdir node_modules
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
  gradle clean
}

buildNode () {
  if [ "$MODULE" = "" ] || [ ! "$MODULE" = "admin" ]; then
    if [ "$BRANCH_NAME" = 'master' ] || [ "$BRANCH_NAME" = 'fix' ]; then
        echo "[buildNode] Use entcore version from package.json ($BRANCH_NAME)"
        case `uname -s` in
          MINGW*)
            npm install --no-bin-links && npm update ode-ts-client ode-ngjs-front && npm update entcore && node_modules/gulp/bin/gulp.js build $NODE_OPTION
            ;;
          *)
            npm install --legacy-peer-deps && npm update ode-ts-client ode-ngjs-front --legacy-peer-deps && npm update entcore--legacy-peer-deps  && node_modules/gulp/bin/gulp.js build $NODE_OPTION --springboard=/home/node/$SPRINGBOARD
        esac
    else
        echo "[buildNode] Use entcore tag $BRANCH_NAME"
        case `uname -s` in
          MINGW*)
            npm install --no-bin-links && npm update ode-ts-client ode-ngjs-front && npm rm --no-save entcore && npm install --no-save entcore@$BRANCH_NAME && node_modules/gulp/bin/gulp.js build $NODE_OPTION
            ;;
          *)
            npm install --legacy-peer-deps && npm update ode-ts-client ode-ngjs-front --legacy-peer-deps && npm rm --no-save entcore --legacy-peer-deps && npm install --no-save entcore@$BRANCH_NAME --legacy-peer-deps && node_modules/gulp/bin/gulp.js build $NODE_OPTION --springboard=/home/node/$SPRINGBOARD
        esac
    fi
  fi
}

buildAdminNode() {
  if [ "$MODULE" = "" ] || [ "$MODULE" = "admin" ]; then
    DEFAULT_PATH=$PWD
    cd admin/src/main/ts
    case `uname -s` in
      MINGW*)
        cd admin/src/main/ts
        npm install --no-bin-links && npm rm --no-save ngx-ode-core ngx-ode-sijil ngx-ode-ui && npm install --no-save ngx-ode-core@$BRANCH_NAME ngx-ode-sijil@$BRANCH_NAME ngx-ode-ui@$BRANCH_NAME && npm run build-prod
        ;;
      *)
        npm install --legacy-peer-deps && npm rm --no-save ngx-ode-core ngx-ode-sijil ngx-ode-ui --legacy-peer-deps && npm install --no-save ngx-ode-core@$BRANCH_NAME ngx-ode-sijil@$BRANCH_NAME ngx-ode-ui@$BRANCH_NAME --legacy-peer-deps && npm run build-prod --legacy-peer-deps
    esac
    cd $DEFAULT_PATH
  fi
}

buildGradle () {
  gradle "$GRADLE_OPTION"shadowJar "$GRADLE_OPTION"install "$GRADLE_OPTION"publishToMavenLocal
}

localDep () {
  for dep in ode-ts-client ode-ngjs-front ; do
    if [ -e $PWD/../$dep ]; then
      rm -rf $dep.tar $dep.tar.gz
      mkdir $dep.tar && mkdir $dep.tar/dist \
        && cp -R $PWD/../$dep/dist $PWD/../$dep/package.json $dep.tar
      tar cfzh $dep.tar.gz $dep.tar
      npm install --no-save $dep.tar.gz
      rm -rf $dep.tar $dep.tar.gz
    fi
  done
}

watch () {
  node_modules/gulp/bin/gulp.js watch-$MODULE --springboard=$SPRINGBOARD
}

ngWatch () {
  DEFAULT_PATH=$PWD
  cd admin/src/main/ts
  npm run start
  cd $DEFAULT_PATH
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
    *)
      echo "Invalid argument : $param"
  esac
  if [ ! $? -eq 0 ]; then
    exit 1
  fi
done

