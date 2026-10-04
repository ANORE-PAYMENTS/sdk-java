#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
build_dir=$(mktemp -d)
trap 'rm -rf -- "$build_dir"' EXIT
find src/main/java src/test/java -name '*.java' -print > "$build_dir/sources.txt"
javac --release 11 -encoding UTF-8 -d "$build_dir/classes" @"$build_dir/sources.txt"
java -cp "$build_dir/classes" cc.anore.ContractTest
