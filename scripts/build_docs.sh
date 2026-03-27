#!/bin/sh

./gradlew \
    :internal:dokka:dokkaGenerate

rm -rf docs/api/
cp -r internal/dokka/build/dokka/html/ docs/api/

cp CHANGELOG.md docs/changelog.md

zensical $@ --clean