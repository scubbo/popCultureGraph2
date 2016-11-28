#!/bin/sh

set -e

gradle shadowJar
java -cp build/libs/popculturegraph-1.0-SNAPSHOT-all.jar -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/popCultureGraph/heapdump org.scubbo.popculturegraph.database.MigrationScript
