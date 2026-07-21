#!/bin/sh

./gradlew \
    :internal:dokka:dokkaGenerate

rm -rf docs/api/
cp -r internal/dokka/build/dokka/html/ docs/api/

cp CHANGELOG.md docs/changelog.md

DEVVIEW_VERSION=${DEVVIEW_VERSION:-$(grep "^VERSION_NAME=" gradle.properties | cut -d'=' -f2 | sed 's/-SNAPSHOT//')}
find docs -name "*.md" -exec sed -i "s/DEVVIEW_VERSION/$DEVVIEW_VERSION/g" {} \;

if [ "$1" != "--prep-only" ]; then
    zensical $@ --clean
fi