#!/bin/sh

set -e

gradle shadowJar
java -cp build/libs/popculturegraph-1.0-SNAPSHOT-all.jar org.scubbo.popculturegraph.server.Startup
