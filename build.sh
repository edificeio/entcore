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

# Maven -U (force the update of the SNAPSHOT dependencies) only in CI (Jenkins
# sets JENKINS_URL): locally, it slows every build down for nothing.
MVN_UPDATE_OPTION=""
if [ "$CI" = "true" ] || [ -n "$JENKINS_URL" ]
then
  MVN_UPDATE_OPTION="-U"
  echo "[buildNode] CI build: maven updates the SNAPSHOT dependencies (-U)"
fi

case `uname -s` in
  MINGW* | Darwin*)
    USER_UID=1000
    GROUP_GID=1000
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
    NODE_OPTION=""
else
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
  	docker compose -f build.compose.yaml create edifice-cli || { rm -f build.compose.yaml; die "init: cannot create the edifice-cli container"; }
  	docker compose -f build.compose.yaml cp edifice-cli:/root/edifice ./edifice || { docker compose -f build.compose.yaml rm -fsv edifice-cli; rm -f build.compose.yaml; die "init: cannot copy edifice-cli"; }
  	docker compose -f build.compose.yaml rm -fsv edifice-cli
  	rm -f build.compose.yaml
  	chmod +x edifice
  	./edifice version $EDIFICE_CLI_DEBUG_OPTION
}

clean () {
  if [ "$NO_DOCKER" = "true" ] ; then
    mvn $MVN_OPTS clean
  else
    docker compose run --rm maven mvn $MVN_OPTS clean
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

# Installs the pnpm workspace (pnpm-workspace.yaml) once for every frontend:
# - the committed lockfile pins every dependency (--frozen-lockfile);
# - update:edifice moves the internal dependencies (catalog dist-tags) to their
#   latest publication, without touching package.json nor the catalog;
# - the legacy libs take the dist-tag of the branch (or FRONT_TAG) when they
#   publish one (scripts/front-legacy-tags.sh).
installFrontend () {
  install_start=$SECONDS
  # The pnpm store lives outside the workspace, which the Jenkins checkout
  # cleans (git clean -ffdx). Created here, so that docker does not create the
  # mounted directory as root.
  mkdir -p "${PNPM_STORE_DIR:-$HOME/.pnpm-store}" || die "cannot create the pnpm store"
  node_run node . "pnpm install --frozen-lockfile && pnpm run update:edifice" || die "frontends: pnpm install failed"
  if [ "$BRANCH_NAME" = 'master' ] || [ "$BRANCH_NAME" = 'fix' ] || [ "$BRANCH_NAME" = 'release' ]; then
    echo "[buildNode] Use the catalog versions of the legacy libs ($BRANCH_NAME)"
  else
    node_run node . "sh scripts/front-legacy-tags.sh $BRANCH_NAME" || die "frontends: cannot apply the '$BRANCH_NAME' tag to the legacy libs"
  fi
  timing "front install (pnpm workspace)" $install_start
}

buildFrontend () {
  # --- What this build needs
  # The gulpfile builds the AngularJS frontends of the modules that have a
  # webpack.config.js (archive, auth, directory, timeline, workspace).
  local build_angularjs="" build_admin="" react_dirs=() modules module dir
  if [ "$MODULE" = "" ] || [ -e ./"$MODULE"/webpack.config.js ]; then
    build_angularjs=true
  fi
  if [ "$MODULE" = "" ] || [ "$MODULE" = "admin" ]; then
    build_admin=true
  fi
  if [ "$MODULE" = "" ]; then
    modules=($(ls -d */ | cut -f1 -d'/'))
  else
    modules=($MODULE)
  fi
  for module in "${modules[@]}"; do
    for dir in frontend frontend-crna; do
      if [ -e ./"$module"/$dir/package.json ]; then
        react_dirs+=("$module/$dir")
      fi
    done
  done
  if [ -z "$build_angularjs" ] && [ -z "$build_admin" ] && [ ${#react_dirs[@]} -eq 0 ]; then
    echo "[buildNode] No frontend to build for module $MODULE"
    return 0
  fi

  installFrontend

  # --- AngularJS frontends (gulp)
  if [ -n "$build_angularjs" ]; then
    if [ "$NO_DOCKER" = "true" ]; then
      springboard_path="../$SPRINGBOARD"
    else
      springboard_path="/home/node/$SPRINGBOARD"
    fi
    gulp_start=$SECONDS
    node_run node . "node_modules/gulp/bin/gulp.js build $NODE_OPTION --springboard=$springboard_path" || die "AngularJS frontends: gulp build failed"
    timing "front AngularJS gulp build" $gulp_start
  fi

  # --- React frontends, built concurrently by pnpm (4 at a time by default)
  if [ ${#react_dirs[@]} -gt 0 ]; then
    react_start=$SECONDS
    filters=""
    for dir in "${react_dirs[@]}"; do
      filters="$filters --filter ./$dir"
    done
    node_run node . "pnpm $filters run build" || die "React frontends: build failed (${react_dirs[*]})"
    timing "front React build (${react_dirs[*]})" $react_start
    # Copy each build into the resources of its module: <module>/backend for
    # the modules whose backend lives there (conversation, portal), the module
    # itself otherwise (compatibility with the legacy frontends of auth and
    # timeline).
    for dir in "${react_dirs[@]}"; do
      module=${dir%%/*}
      if [ -e ./"$module"/backend ]; then
        sync_target="../backend"
      else
        sync_target=".."
      fi
      (cd ./"$dir" && syncReactFrontendBuildToResources "$sync_target" && rm -rf ./dist) \
        || die "cannot copy the build of $dir"
    done
  fi

  # --- Angular admin
  if [ -n "$build_admin" ]; then
    admin_start=$SECONDS
    node_run node . "pnpm --filter admin-app run build-prod" || die "admin: build failed"
    timing "front Angular admin build" $admin_start
  fi
}

buildBackend () {
  maven_start=$SECONDS
  if [ "$NO_DOCKER" = "true" ] ; then
    mvn $MVN_OPTS install -DskipTests
  else
    docker compose run --rm maven mvn $MVN_OPTS install -DskipTests $MVN_UPDATE_OPTION
  fi
  timing "backend maven install" $maven_start
}

install () {
  maven_start=$SECONDS
  docker compose run $CI_OPTION --rm maven mvn $MVN_OPTS clean install -DskipTests $MVN_UPDATE_OPTION
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

# localDep and infra used to `npm install` a local build of a legacy lib
# (ode-ts-client, ode-ngjs-front, infra-front) into the root node_modules, which
# npm cannot do since pnpm manages them (ENABLING-1397).
removedLocalLibCommand () {
  die "'$1' has been removed: npm cannot install into the pnpm workspace. To test a local build of a legacy lib: \`pnpm link ../<lib>\` at the root, build with --no-docker (the lib is not mounted in the node container), then come back with \`git checkout -- package.json pnpm-workspace.yaml pnpm-lock.yaml && pnpm install\`."
}

watch () {
  docker compose run --rm maven sh -c "mvn $MVN_OPTS help:evaluate -Dexpression=project.groupId -q -DforceStdout -pl $MODULE && echo -n '~$MODULE~' &&  mvn $MVN_OPTS help:evaluate -Dexpression=project.version -pl $MODULE -q -DforceStdout" > .version.properties
  docker compose run --rm \
    -v $PWD/../$SPRINGBOARD:/home/node/$SPRINGBOARD \
    node sh -c "npx gulp watch-$MODULE $NODE_OPTION --springboard=../$SPRINGBOARD "
  rm -f .version.properties
}

# ex: ./build.sh -m=workspace -s=paris watch

ngWatch () {
  docker compose run --rm -u "$USER_UID:$GROUP_GID" $CI_OPTION --publish 4200:4200 node sh -c "pnpm --filter admin-app run dev"
}

publish() {
  version=`docker compose run --rm maven mvn $MVN_OPTS help:evaluate -Dexpression=project.version -q -DforceStdout`
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

# Skipped when the first command is init (Jenkins: `init clean install`), which
# would otherwise run twice in a row.
if [ ! -e .env ] && [ "$1" != "init" ]; then
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
    localDep|infra)
      removedLocalLibCommand "$param"
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