#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
export KAFKA_HOME=~/Documents/kafka_2_12

echo "Dir = $DIR"

mkdir -p $DIR/logs

${DIR}/shutdown.bash
pwd
cd $KAFKA_HOME
pwd

echo "+Resetting application state"
rm -rf /tmp/kafka-streams/my-first-streams-application

#echo "+Application reset1"
#bin/kafka-streams-application-reset.sh --application-id "my-first-streams-application"



echo "+Starting Zookeeper"
(bin/zookeeper-server-start.sh config/zookeeper.properties) 2>&1 >${DIR}/logs/zoo.log &
sleep 10

#echo "+Application reset2"
#bin/kafka-streams-application-reset.sh --application-id "my-first-streams-application"



echo "+Starting Broker"
(bin/kafka-server-start.sh config/server.properties) 2>&1 >${DIR}/logs/broker.log &
sleep 10
#
#echo "+Deleting all consumer groups"
#bin/kafka-consumer-groups.sh --delete --all-groups --bootstrap-server localhost:9092

echo "setting up consumer groups"
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --all-groups --reset-offsets --to-latest --all-topics  --execute

echo "+Application reset"
bin/kafka-streams-application-reset.sh --application-id ""

