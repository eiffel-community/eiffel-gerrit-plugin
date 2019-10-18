#!/usr/bin/env bash

export HOST=$(hostname -I | tr " " "\n"| head -1);
echo "Docker Host IP: $HOST"

export RABBITMQ_IMAGE="bitnami/rabbitmq:3.7.8-debian-9"
export REMREM_GENERATE_IMAGE="eiffelericsson/eiffel-remrem-generate:2.0.4"
export REMREM_PUBLISH_IMAGE="eiffelericsson/eiffel-remrem-publish:2.0.2"

export RABBITMQ_AMQP_PORT=5672
export RABBITMQ_WEB_PORT=15672
export REMREM_GENERATE_PORT=8095
export REMREM_PUBLISH_PORT=8096

export REMREM_PUBLISH_RABBITMQ_INSTANCES_LIST="[\
{ \"mp\": \"eiffelsemantics\", \"host\": \"rabbitmq\", \"port\": \"5672\", \"username\": \"myuser\", \"password\": \"myuser\", \"tls\": \"\", \"exchangeName\": \"ei-exchange\", \"domainId\": \"ei-domain\", \"createExchangeIfNotExisting\":true }\
]"
