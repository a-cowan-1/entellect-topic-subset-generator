#!/bin/bash

echo "Shutting down Zookeper"

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd $KAFKA_HOME

echo "--Stopping Broker"
bin/kafka-server-stop.sh 2>&1 >${DIR}/logs/broker-shutdown.log

echo "--Stopping Zookeeper"
bin/zookeeper-server-stop.sh 2>&1 >${DIR}/logs/zoo-shutdown.log



