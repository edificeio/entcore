#!/bin/bash

# Options
NO_DOCKER=""
for i in "$@"
do
case $i in
  --no-docker*)
  NO_DOCKER="true"
  shift
  ;;
  *)
  ;;
esac
done

if [ "$#" -lt 1 ]; then
  echo "Usage: $0 <clean|init|localDep|build|install|watch>"
  echo "Example: $0 clean"
  echo "Example: $0 init"
  echo "Example: $0 build"
  exit 1
fi

if [[ "$*" == *"--no-user"* ]]
then
  USER_OPTION=""
else
  case `uname -s` in
    MINGW* | Darwin*)
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
  USER_OPTION="-u $USER_UID:$GROUP_GID"
fi

# options
SPRINGBOARD="recette"
for i in "$@"
do
case $i in
    -s=*|--springboard=*)
    SPRINGBOARD="${i#*=}"
    shift
    ;;
    *)
    ;;
esac
done

clean () {
  rm -rf .nx
  rm -rf node_modules 
  rm -rf dist 
  rm -rf build 
  rm -rf .pnpm-store
  # rm -f package.json 
  rm -f pnpm-lock.yaml
}

init() {
  if [ "$NO_DOCKER" = "true" ] ; then
    pnpm install
  else
    docker-compose run -e NPM_TOKEN -e TIPTAP_PRO_TOKEN --rm $USER_OPTION node sh -c "pnpm install"
  fi
}

build () {
  if [ "$NO_DOCKER" = "true" ] ; then
    pnpm run build
  else
    docker-compose run -e NPM_TOKEN -e TIPTAP_PRO_TOKEN --rm $USER_OPTION node sh -c "pnpm build"
  fi
  status=$?
  if [ $status != 0 ];
  then
    exit $status
  fi
}

linkDependencies () {
  # Check if the edifice-frontend-framework directory exists
  if [ ! -d "$PWD/../../edifice-frontend-framework/packages" ]; then
    echo "Directory edifice-frontend-framework/packages does not exist."
    exit 1
  else
    echo "Directory edifice-frontend-framework/packages exists."
  fi


  # # Extract dependencies from package.json using sed
  DEPENDENCIES=$(sed -n '/"dependencies": {/,/}/p' package.json | sed -n 's/ *"@edifice\.io\/\([^"]*\)":.*/\1/p')

  # # Link each dependency if it exists in the edifice-frontend-framework
  for dep in $DEPENDENCIES; do
    # Handle special case for ts-client
    package_path="$PWD/../../edifice-frontend-framework/packages/$dep"

    if [ -d "$package_path" ]; then
      echo "Linking package: $dep"
      (cd "$package_path" && pnpm link --global)
    else
      echo "Package $dep not found in edifice-frontend-framework."
    fi
  done

  # Check if ode-explorer exists in package.json using sed
  if [ -n "$(sed -n '/"ode-explorer":/p' package.json)" ]; then
    echo "ode-explorer found in package.json"
    
    # Check if explorer frontend path exists
    if [ -d "$PWD/../../explorer/frontend" ]; then
      echo "explorer/frontend directory exists"
      echo "Linking ode-explorer globally..."
      (cd "$PWD/../../explorer/frontend" && pnpm link --global)
      pnpm link --global ode-explorer
    else
      echo "explorer/frontend directory not found"
      exit 1
    fi
  else
    echo "ode-explorer not found in package.json"
  fi

  # # Link the packages in the current application
  echo "Linking packages in the current application..."
  Link each dependency from package.json
  for dep in $DEPENDENCIES; do
    pnpm link --global "@edifice.io/$dep"
  done

  echo "All specified packages have been linked successfully."
}

cleanDependencies() {
  rm -rf node_modules && rm -f pnpm-lock.yaml && pnpm install
}

for param in "$@"
do
  case $param in
    clean)
      clean
      ;;
    init)
      init
      ;;
    build)
      build
      ;;
    linkDependencies)
      linkDependencies
      ;;
    cleanDependencies)
      cleanDependencies
      ;;
    *)
      echo "Invalid argument : $param"
  esac
  if [ ! $? -eq 0 ]; then
    exit 1
  fi
done