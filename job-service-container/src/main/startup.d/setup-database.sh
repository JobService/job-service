#!/bin/bash
#
# Copyright 2016-2024 Open Text.
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


cd /maven
echo "Installing Job Service database."

java -cp "*" \
     com.github.cafapi.util.flywayinstaller.Application \
     -db.host "${JOB_SERVICE_DATABASE_HOST}" \
     -db.port "${JOB_SERVICE_DATABASE_PORT}" \
     -db.user "${JOB_SERVICE_DATABASE_USERNAME}" \
     -db.pass "${JOB_SERVICE_DATABASE_PASSWORD}" \
     -db.name "${JOB_SERVICE_DATABASE_NAME}"

echo "Completed installation of Job Service database."
