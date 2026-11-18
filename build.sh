#!/bin/bash

if [ ! -e node_modules ]
then
  mkdir node_modules
fi

# Set CI_OPTION to " -T " if running in CI environment
CI_OPTION=""
if [ ! -z "$CI" ] && [ "$CI" = "true" ]
then
  CI_OPTION=" -T "
fi

NPM_INSTALL="npm install"
case `uname -s` in
  MINGW* | Darwin*)
    USER_UID=1000
    GROUP_GID=1000
    case `uname -s` in
      # npm cannot create the bin links on Windows.
      MINGW*) NPM_INSTALL="npm install --no-bin-links" ;;
    esac
    ;;
  *)
    if [ -z ${USER_UID:+x} ]
    then
      USER_UID=`id -u`
    fi
    if [ -z ${GROUP_GID:+x} ]
    then
      GROUP_GID=`id -g`
    fi
esac

# build options
NO_DOCKER=""
SPRINGBOARD="recette"
# Initialize MODULE from env; --module=/-m= overrides it.
MODULE="${MODULE:-}"
MVN_OPTS="-Duser.home=/var/maven -T 4"
# Options are accepted at any position: they are removed from the positional
# parameters, and only the commands (init, clean, install...) are kept.
COMMANDS=()
for i in "$@"
do
case $i in
  --no-docker*)
    NO_DOCKER="true"
    MVN_OPTS=""
    ;;
  -s=*|--springboard=*)
    SPRINGBOARD="${i#*=}"
    ;;
  -m=*|--module=*)
    MODULE="${i#*=}"
    ;;
  *)
    COMMANDS+=("$i")
    ;;
esac
done
set -- "${COMMANDS[@]}"

if [ "$MODULE" = "" ]; then
    GRADLE_OPTION=""
    NODE_OPTION=""
else
  GRADLE_OPTION=":$MODULE:"
  NODE_OPTION="--module $MODULE"
  if [ -e "$MODULE/backend" ]; then
    echo "BACKEND SUB-PROJECT $MODULE/backend DETECTED"
    MVN_OPTS="$MVN_OPTS --projects $MODULE/backend -am"
  else
    MVN_OPTS="$MVN_OPTS --projects $MODULE -am"
  fi
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

# Logs how long a build phase took, measured from the given start ($SECONDS),
# and returns the exit status of the command run just before, so that wrapping
# a phase does not change how its failures are handled.
# Usage: start=$SECONDS; <phase>; timing "<label>" $start
timing () {
  local status=$?
  echo "[timing] $1: $((SECONDS - $2))s"
  return $status
}

# Stops the whole build with an explicit message. Used instead of `set -e`,
# which bash ignores inside functions called from a `&&` list, such as
# `buildFrontend && buildBackend`.
die () {
  echo "[error] $*" >&2
  exit 1
}

# Runs a shell command with Node: in the given docker compose service, or
# directly in the given directory with --no-docker.
# Usage: node_run <service> <local directory> <command>
node_run () {
  if [ "$NO_DOCKER" = "true" ]; then
    (cd "$2" && sh -c "$3")
  else
    docker compose run --rm -u "$USER_UID:$GROUP_GID" $CI_OPTION "$1" sh -c "$3"
  fi
}

init() {
  me=`id -u`:`id -g`
  echo "DEFAULT_DOCKER_USER=$me" > .env

    # If CLI_VERSION is empty set $cli_version to latest
  	if [ -z "$CLI_VERSION" ]; then
  		CLI_VERSION="latest"
  	fi
  	# Create a build.compose.yaml file from following template
  	cat <<EOF > build.compose.yaml
services:
  edifice-cli:
    image: opendigitaleducation/edifice-cli:$CLI_VERSION
    user: "$DEFAULT_DOCKER_USER"
EOF
  	# Copy /root/edifice from edifice-cli container to host machine
  	docker compose -f build.compose.yaml create edifice-cli || die "init: cannot create the edifice-cli container"
  	docker compose -f build.compose.yaml cp edifice-cli:/root/edifice ./edifice || die "init: cannot copy edifice-cli"
  	docker compose -f build.compose.yaml rm -fsv edifice-cli
  	rm -f build.compose.yaml
  	chmod +x edifice
  	./edifice version $EDIFICE_CLI_DEBUG_OPTION
}

clean () {
  if [ "$NO_DOCKER" = "true" ] ; then
    mvn $MVN_OPTS clean
  else
    docker compose run --rm $USER_OPTION maven mvn $MVN_OPTS clean
  fi
}

syncReactFrontendBuildToResources() {
  sync_target_root="$1"
  sync_resources_dir="$sync_target_root/src/main/resources"
  sync_public_dir="$sync_resources_dir/public"
  sync_view_dir="$sync_resources_dir/view"

  # Move dist to resources dir
  cp -R ./dist/* "$sync_resources_dir/" || return 1

  # Create resources/view directory if needed
  mkdir -p "$sync_view_dir" || return 1

  # Move generated HTML entrypoints into the view directory when present.
  for html_file in "$sync_resources_dir"/*.html; do
    [ -f "$html_file" ] || continue
    echo "...moving HTML file $html_file to $sync_view_dir/"
    mv "$html_file" "$sync_view_dir/" || return 1
  done

  unset sync_target_root sync_resources_dir sync_public_dir sync_view_dir file file_name html_file
}

buildFrontend () {

  # --- Build angularJS-based frontends
  # The gulpfile builds every module that has a webpack.config.js (archive,
  # auth, directory, timeline, workspace). With any other module, gulp would
  # silently fall back to building all of them.
  if [ "$MODULE" = "" ] || [ -e ./"$MODULE"/webpack.config.js ]; then
    angularjs_start=$SECONDS
    if [ "$NO_DOCKER" = "true" ]; then
      springboard_path="../$SPRINGBOARD"
    else
      springboard_path="/home/node/$SPRINGBOARD"
    fi
    if [ "$BRANCH_NAME" = 'master' ] || [ "$BRANCH_NAME" = 'fix' ]  || [ "$BRANCH_NAME" = 'release' ]; then
      echo "[buildNode] Use entcore version from package.json ($BRANCH_NAME)"
      node_run node . "$NPM_INSTALL && npm update entcore" || die "AngularJS frontends: npm install failed"
    else
      echo "[buildNode] Use entcore tag $BRANCH_NAME"
      # A feature branch has no tag of its own on these libs unless FRONT_TAG
      # is set: the versions of package.json are then kept, as before, but the
      # build now says so instead of ignoring the error.
      node_run node . "npm rm --no-save entcore ode-ts-client ode-ngjs-front && npm install --no-save entcore@$BRANCH_NAME ode-ts-client@$BRANCH_NAME ode-ngjs-front@$BRANCH_NAME" \
        || echo "[buildNode] WARNING: no '$BRANCH_NAME' tag for entcore, ode-ts-client or ode-ngjs-front: using the versions of package.json (set FRONT_TAG to choose a tag)"
      node_run node . "$NPM_INSTALL" || die "AngularJS frontends: npm install failed"
    fi
    timing "front AngularJS install" $angularjs_start
    gulp_start=$SECONDS
    node_run node . "node_modules/gulp/bin/gulp.js build $NODE_OPTION --springboard=$springboard_path" || die "AngularJS frontends: gulp build failed"
    timing "front AngularJS gulp build" $gulp_start
    timing "front AngularJS (npm install + gulp)" $angularjs_start
  fi

  # --- Build react-based frontends
  local modules
  if [ "$MODULE" = "" ]; then
    modules=($(ls -d */ | cut -f1 -d'/'))
  else 
    modules=($MODULE)
  fi
  
  for module in "${modules[@]}"; do
    if [ -e ./"$module"/frontend ]; then
      echo -e "[Build React] Build react frontend for module $module"
      cd ./"$module"/frontend || die "cannot enter $module/frontend"
      react_start=$SECONDS
      if [ "$NO_DOCKER" = "true" ] ; then
        ./build.sh --no-docker clean init build
      else
        ./build.sh clean init build
      fi
      timing "front React $module/frontend (clean + install + build)" $react_start
      if [ $? -ne 0 ]; then
        die "Error while building React frontend for module $module"
      fi

      # Create directory structure and copy frontend build files.
      if [ "$module" = "timeline" ] || [ "$module" = "auth" ]; then
        # compatibility mode : preserve legacy frontends files in ../src
        syncReactFrontendBuildToResources ".." || die "cannot copy the build of $module/frontend"
      else
        syncReactFrontendBuildToResources "../backend" || die "cannot copy the build of $module/frontend"
      fi

      # Clean up
      rm -rf ./dist
      cd ../..
    fi
    if [ -e ./"$module"/frontend-crna ]; then
      echo -e "[Build React] Build react frontend-crna for module $module"
      cd ./"$module"/frontend-crna || die "cannot enter $module/frontend-crna"
      react_start=$SECONDS
      if [ "$NO_DOCKER" = "true" ] ; then
        ./build.sh --no-docker clean init build
      else
        ./build.sh clean init build
      fi
      timing "front React $module/frontend-crna (clean + install + build)" $react_start
      if [ $? -ne 0 ]; then
        die "Error while building React frontend-crna for module $module"
      fi
      syncReactFrontendBuildToResources ".." || die "cannot copy the build of $module/frontend-crna"
      rm -rf ./dist
      cd ../..
    fi
  done

  # --- Build angular-based frontends
  if [ "$MODULE" = "" ] || [ "$MODULE" = "admin" ]; then
    admin_start=$SECONDS
    admin_dir=admin/src/main/ts
    node_run node16 $admin_dir "$NPM_INSTALL" || die "admin: npm install failed"
    # Same fallback as the AngularJS frontends when the libs have no tag for
    # this branch. Before, the whole admin build failed in that case.
    node_run node16 $admin_dir "npm rm --no-save ngx-ode-core ngx-ode-sijil ngx-ode-ui && npm install --no-save ngx-ode-core@$BRANCH_NAME ngx-ode-sijil@$BRANCH_NAME ngx-ode-ui@$BRANCH_NAME" \
      || {
        echo "[buildNode] WARNING: no '$BRANCH_NAME' tag for ngx-ode-core, ngx-ode-sijil or ngx-ode-ui: using the versions of package.json (set FRONT_TAG to choose a tag)"
        node_run node16 $admin_dir "$NPM_INSTALL" || die "admin: npm install failed"
      }
    timing "front Angular admin install" $admin_start
    admin_build_start=$SECONDS
    # Outside docker, admin/src/main/ts and admin/ are not mounted on
    # /home/node/app and /home/node/base: the relative paths of build-prod apply.
    if [ "$NO_DOCKER" = "true" ]; then
      admin_build_script=build-prod
    else
      admin_build_script=build-docker-prod
    fi
    node_run node16 $admin_dir "npm run $admin_build_script" || die "admin: build failed"
    timing "front Angular admin build" $admin_build_start
    timing "front Angular admin (npm install + build)" $admin_start
  fi
}

buildBackend () {
  maven_start=$SECONDS
  if [ "$NO_DOCKER" = "true" ] ; then
    mvn $MVN_OPTS install -DskipTests
  else
    docker compose run --rm $USER_OPTION maven mvn $MVN_OPTS install -DskipTests -U
  fi
  timing "backend maven install" $maven_start
}

install () {
  maven_start=$SECONDS
  docker compose run $CI_OPTION --rm maven mvn $MVN_OPTS clean install -DskipTests -U
  timing "backend maven clean install" $maven_start || die "maven build failed"
  # The final `cd -` used to hide the result of the broker client build.
  cd broker-parent/broker-client/quarkus || die "cannot enter broker-parent/broker-client/quarkus"
  ./build.sh install || die "broker client build failed"
  cd - > /dev/null
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
      docker compose run --rm -u "$USER_UID:$GROUP_GID" $CI_OPTION node sh -c "npm install --no-save $dep.tar.gz"
      rm -rf $dep.tar $dep.tar.gz
    fi
  done
}

watch () {
  docker compose run --rm maven sh -c "mvn $MVN_OPTS help:evaluate -Dexpression=project.groupId -q -DforceStdout -pl $MODULE && echo -n '~$MODULE~' &&  mvn $MVN_OPTS help:evaluate -Dexpression=project.version -pl $MODULE -q -DforceStdout" > .version.properties  
  docker compose run --rm \
    $USER_OPTION \
    -v $PWD/../$SPRINGBOARD:/home/node/$SPRINGBOARD \
    node sh -c "npx gulp watch-$MODULE $NODE_OPTION --springboard=../$SPRINGBOARD "
  rm -f .version.properties
}

# ex: ./build.sh -m=workspace -s=paris watch

ngWatch () {
  docker compose run --rm -u "$USER_UID:$GROUP_GID" $CI_OPTION --publish 4200:4200 node16 sh -c "npm run dev"
}

infra () {
  docker compose run --rm -u "$USER_UID:$GROUP_GID" $CI_OPTION node sh -c "npm install /home/node/infra-front"
}

publish() {
  version=`docker compose run --rm $USER_OPTION maven mvn $MVN_OPTS help:evaluate -Dexpression=project.version -q -DforceStdout`
  level=`echo $version | cut -d'-' -f3`
  case "$level" in
    *SNAPSHOT) export nexusRepository='snapshots' ;;
    *)         export nexusRepository='releases' ;;
  esac

  docker compose run --rm  maven mvn $MVN_OPTS -DrepositoryId=ode-$nexusRepository -DskipTests --settings /var/maven/.m2/settings.xml deploy
}

image() {
  ./edifice image --project-type=entcore --rebuild=false $EDIFICE_CLI_DEBUG_OPTION
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
  cd $script_dir/tests/src/test/js
  docker compose run --rm -T node18 pnpm i --force
  cd $script_dir/tests/src/test/js/it/scenarios
  failed_files=()
  exit_code=0
  js_files=($(find . -type f -name '*.ts' ! -name '_*'))
  for it_file in "${js_files[@]}"; do
    short_file_name=$(basename -s .ts $it_file)
    file_dir=$(dirname $it_file)
    check_prefix_sh_file "$file_dir" "$short_file_name"
    if [ $? -eq 1 ]; then
      echo executing $it_file
      docker compose run --rm -T k6 run --compatibility-mode=experimental_enhanced file:///home/k6/src/it/scenarios/$it_file
      if [ $? -ne 0 ]; then
          exit_code=1
          failed_files+=("$it_file")
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
        failed_files+=("$sh_file")
        echo "Error while executing : $sh_file"
    fi
  done
  cd -

  # Output summary of failed files
  if [ ${#failed_files[@]} -ne 0 ]; then
    echo "|-------------------------|"
    echo "|--- FAILED TEST FILES ---|"
    for failed in "${failed_files[@]}"; do
      echo "| $failed"
    done
    echo "|-------------------------|"
  fi
  
  echo "|-------------------------|"
  [ $exit_code -ne 0 ] && echo "|---- itTests  FAILED ----|" || echo "|--- itTests SUCCEEDED ---|"
  echo "|-------------------------|"
  exit $exit_code
}

if [ ! -e .env ]; then
  # Run in a subshell so that a failure does not stop the build: this
  # automatic init writes .env first, and edifice-cli is only needed by
  # `image`. The image has no arm64 variant, so it fails on Apple Silicon.
  (init) || echo "[buildNode] WARNING: automatic init failed, .env is written but edifice-cli is unavailable (only needed by the image command)"
fi

for param in "$@"
do
  command_start=$SECONDS
  case $param in
    '--no-user')
      ;;
    init)
      init
      ;;
    clean)
      clean
      ;;
    buildFrontend)
      buildFrontend
      ;;
    buildBackend)
      buildBackend
      ;;
    buildMvn)
      install
      ;;
    install)
      buildFrontend && buildBackend
      ;;
    buildBack)
      install
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
    image)
      image
      ;;
    *)
      # A typo used to do nothing and still end the build successfully.
      die "Invalid argument : $param"
  esac
  timing "command $param" $command_start
  if [ ! $? -eq 0 ]; then
    exit 1
  fi
done
echo "[timing] total: ${SECONDS}s"