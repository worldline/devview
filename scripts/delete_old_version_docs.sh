#!/bin/bash

set -eu

# Parse mike list output. Format: "version [alias1, alias2]  title"
# We only care about version names (first column) and aliases.
# Aliases like 'latest' and 'dev' must never be deleted.

latest_alias_version=""
dev_alias_version=""
bare_versions=()
versions=$(mike list)

while IFS= read -r line; do
  version=$(echo "$line" | awk '{print $1}')
  version_aliases=$(echo "$line" | grep -o '\[[^]]*\]' | head -n1 | tr -d '[]' | tr ',' '\n' | xargs)

  bare_versions+=("$version")

  for alias in $version_aliases; do
    case "$alias" in
      latest) latest_alias_version="$version" ;;
      dev) dev_alias_version="$version" ;;
    esac
  done
done <<< "$versions"

declare -A major_latest

for v in "${bare_versions[@]}"; do
  [[ "$v" =~ ^[0-9]+[.][0-9]+[.][0-9]+(-[a-zA-Z0-9._-]+)?$ ]] || continue
  major="${v%%-*}"
  major="${major%.*}"
  if [[ -z "${major_latest[$major]:-}" ]]; then
    major_latest[$major]="$v"
  else
    prev="${major_latest[$major]}"
    if [[ "${prev%%-*}" == "${v%%-*}" && ( "$prev" != *-* || "$v" != *-* ) ]]; then
      # A final release supersedes every prerelease of the same version.
      major_latest[$major]="${v%%-*}"
    else
      greater=$(printf "%s\n%s\n" "$prev" "$v" | sort -V | tail -n1)
      major_latest[$major]="$greater"
    fi
  fi
done

to_delete=()
for v in "${bare_versions[@]}"; do
  [[ "$v" =~ ^[0-9]+[.][0-9]+[.][0-9]+(-[a-zA-Z0-9._-]+)?$ ]] || continue
  # Never delete the version holding the 'latest' or 'dev' alias
  if [[ "$v" == "$latest_alias_version" ]]; then
    echo "Keeping $v (holds 'latest' alias)"
    continue
  fi
  if [[ "$v" == "$dev_alias_version" ]]; then
    echo "Keeping $v (holds 'dev' alias)"
    continue
  fi

  major="${v%%-*}"
  major="${major%.*}"
  if [[ "$v" != "${major_latest[$major]}" ]]; then
    to_delete+=("$v")
  fi
done

for v in "${to_delete[@]}"; do
  echo "Deleting $v"
  mike delete "$v" --push
done
