#!/usr/bin/env bash

###
# Job Service 
###

## Postgres Database Connection Details
export JOB_SERVICE_DATABASE_HOST=192.168.56.10
export JOB_SERVICE_DATABASE_PORT=5432
export JOB_SERVICE_DATABASE_USERNAME=postgres
export JOB_SERVICE_DATABASE_PASSWORD=root

## Job Service Web Service Connection Details
export JOB_SERVICE_PORT=9411
export JOB_SERVICE_DOCKER_HOST=192.168.56.10

## Job Service Resume Job Queue Details
export CAF_JOB_SERVICE_RESUME_JOB_QUEUE=worker-taskunstowing-in

###
# RabbitMQ
###

## RabbitMQ Connection Details
export CAF_RABBITMQ_HOST=192.168.56.10
export CAF_RABBITMQ_PORT=5672
export CAF_RABBITMQ_USERNAME=guest
export CAF_RABBITMQ_PASSWORD=guest
