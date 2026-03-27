#!/usr/bin/env bash

set -exo pipefail

# Gets a property out of a .properties file
# usage: getProperty $key $filename
function getProperty() {
    grep "${1}" "$2" | cut -d'=' -f2
}

NEW_VERSION=$1
NEW_SNAPSHOT_VERSION=$2
CUR_SNAPSHOT_VERSION=$(getProperty 'VERSION_NAME' gradle.properties)

if [ -z "$NEW_VERSION" ]; then
  echo "Usage: ./release.sh <new-version> [<next-snapshot-version>]"
  echo "Example: ./release.sh 1.0.0 1.1.0-SNAPSHOT"
  exit 1
fi

if [ -z "$NEW_SNAPSHOT_VERSION" ]; then
  # If no snapshot version was provided, keep the current value
  NEW_SNAPSHOT_VERSION=$CUR_SNAPSHOT_VERSION
fi

echo "Preparing release $NEW_VERSION"

# Bump to release version and commit
sed -i.bak "s/${CUR_SNAPSHOT_VERSION}/${NEW_VERSION}/g" gradle.properties
git add gradle.properties
git commit -m "Prepare for release $NEW_VERSION"

# Tag the release — this is what triggers the publish.yml CI workflow
git tag "v${NEW_VERSION}"

# Bump back to next snapshot version and commit
echo "Setting next snapshot version $NEW_SNAPSHOT_VERSION"
sed -i.bak "s/${NEW_VERSION}/${NEW_SNAPSHOT_VERSION}/g" gradle.properties
git add gradle.properties
git commit -m "Prepare next development version"

# Remove the backup file from sed edits
rm gradle.properties.bak

# Push commits and the release tag — CI takes it from here
git push && git push --tags