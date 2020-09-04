#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
export KAFKA_HOME=~/Documents/kafka_2_12
cd ${DIR}/../../
sbt "runMain attempt.GeneratePpData"