#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

WHITE_TEXT='\033[1;37m'
RED_TEXT='\033[0;31m'
cd $KAFKA_HOME


for arg in "$@"
do
  echo -e "${WHITE_TEXT}  Deleting old topic '$arg'${RED_TEXT}"
  bin/kafka-topics.sh --if-exists --delete --zookeeper localhost:2181 --topic $arg

  echo -e "${WHITE_TEXT}  Recreating topic '$arg'${RED_TEXT}"
  bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 2 --topic $arg
  echo -e "${WHITE_TEXT}"

done
