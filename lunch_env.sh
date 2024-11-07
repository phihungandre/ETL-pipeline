#!/bin/bash

# Exit script on first error
set -e

# Change directory to where the Kafka is installed
cd ../../apps/kafka_2.13-3.4.0/bin

# Start Zookeeper
./zookeeper-server-start.sh ../config/zookeeper.properties &
sleep 10 # Wait for Zookeeper to start

# Start Kafka server
./kafka-server-start.sh ../config/server.properties &
sleep 10 # Wait for Kafka server to start

# Create Kafka topics
./kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic report_data &
sleep 5 # Wait for topic creation

./kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic report_alert &
sleep 5 # Wait for topic creation

# Bring script back to the foreground so it doesn't terminate early
wait
