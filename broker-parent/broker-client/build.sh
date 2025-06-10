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

publish () {
  cd $SCRIPT_DIR/nest
  echo "[publish] Publish packages..."
  # Récupération de la branche locale
  LOCAL_BRANCH=`echo $GIT_BRANCH | sed -e "s|origin/||g"`
  # Récupération de la date et du timestamp
  TIMESTAMP=`date +%Y%m%d%H%M%S`
  # Récupération du dernier tag stable
  LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "1.0.0")
  LATEST_TAG=${LATEST_TAG#v}

  # Définition du tag de la branche
  if [ "$LOCAL_BRANCH" = "main" ]; then
    TAG_BRANCH="latest"
  else
    TAG_BRANCH=$LOCAL_BRANCH
  fi


  # Création de la nouvelle version
  if [ "$LOCAL_BRANCH" = "main" ]; then
    NEW_VERSION="$LATEST_TAG"
  else
    # Mettre à jour la version dans tous les packages avec la version exacte
    NEW_VERSION="$LATEST_TAG-$LOCAL_BRANCH.$TIMESTAMP"
    echo "[publish] Update version in all packages with the exact version"
    docker compose run -e NPM_TOKEN=$NPM_TOKEN --rm -u "$USER_UID:$GROUP_GID" node sh -c "pnpm -r exec npm version $NEW_VERSION --no-git-tag-version"
  fi

  # Publier avec le tag de la branche
  echo "[publish] Publish with the branch tag"
  # Default to dry run if not specified
  DRY_RUN=${DRY_RUN:-true}
  
  if [ "$DRY_RUN" = "true" ]; then
    docker compose run -e NPM_TOKEN=$NPM_TOKEN --rm -u "$USER_UID:$GROUP_GID" node sh -c "pnpm publish -r --no-git-checks --tag $TAG_BRANCH --access=public --dry-run"
  else
    docker compose run -e NPM_TOKEN=$NPM_TOKEN --rm -u "$USER_UID:$GROUP_GID" node sh -c "pnpm publish -r --no-git-checks --tag $TAG_BRANCH --access=public"
  fi
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

