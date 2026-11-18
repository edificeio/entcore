#!/bin/sh
# Applies a dist-tag to the legacy frontend libs of the pnpm workspace, without
# touching package.json nor the catalog of pnpm-workspace.yaml (--no-save: only
# the lockfile and node_modules change). Run by build.sh, in the node container,
# after `pnpm install`.
#
# These libs publish one dist-tag per branch (entcore@feat-XXX...). A build uses
# the tag of its branch when a lib publishes it, and the catalog tag otherwise.
# FRONT_TAG (Jenkins parameter) replaces the branch name. See D9 in
# ENABLING-1397-DECISIONS.md.
#
# Usage: scripts/front-legacy-tags.sh <tag>

tag="$1"
if [ -z "$tag" ]; then
  echo "usage: $0 <tag>" >&2
  exit 1
fi

specs=""
for lib in entcore ode-ts-client ode-ngjs-front ngx-ode-core ngx-ode-sijil ngx-ode-ui; do
  if grep -q "^  $lib: $tag\$" pnpm-workspace.yaml; then
    continue # already the catalog tag, applied by update:edifice
  fi
  if [ -n "$(pnpm view "$lib" "dist-tags.$tag" 2>/dev/null)" ]; then
    specs="$specs $lib@$tag"
  else
    echo "[front-legacy-tags] no '$tag' tag for $lib: keeping the catalog version"
  fi
done

if [ -n "$specs" ]; then
  echo "[front-legacy-tags] using:$specs"
  # Word splitting of $specs is intended.
  pnpm --recursive --include-workspace-root update $specs --no-save
fi
