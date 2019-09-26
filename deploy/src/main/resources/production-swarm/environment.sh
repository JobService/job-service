#!/usr/bin/env bash
#
# Copyright 2015-2018 Micro Focus or one of its affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


###
# Job Service 
###

## Postgres Database Connection Details
export JOB_SERVICE_DB_HOST=192.168.56.10
export JOB_SERVICE_DB_PORT=5432
export CAF_DATABASE_USERNAME=postgres
export CAF_DATABASE_PASSWORD=root

## Job Service Web Service Connection Details
export JOB_SERVICE_PORT=9411
export JOB_SERVICE_DOCKER_HOST=192.168.56.10

###
# RabbitMQ
###

## RabbitMQ Connection Details
export CAF_RABBITMQ_HOST=192.168.56.10
export CAF_RABBITMQ_PORT=5672
export CAF_RABBITMQ_USERNAME=guest
export CAF_RABBITMQ_PASSWORD=guest
