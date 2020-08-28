#!/bin/bash


export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
export KAFKA_HOME=~/Documents/kafka_2_12

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd ${DIR}/..

common/server-setup.bash

common/reset-topics.bash pp1 pp2

#echo "Deleting old topics (may fail if broker setup was slow, just rerun"
#bin/kafka-topics.sh --delete --bootstrap-server localhost:9092 --topic fib
#bin/kafka-topics.sh --delete --bootstrap-server localhost:9092 --topic unaggregated
#bin/kafka-topics.sh --delete --bootstrap-server localhost:9092 --topic aggregated
#echo "Creating topics (may fail if broker setup was slow, just rerun"
#bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 2 --topic fib
#bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 2 --topic unaggregated
#bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 2 --topic aggregated

echo "Running Scala code"
cd ${DIR}/../../
pwd
sbt "runMain materialise.RunTestConsumer"

