#!/bin/bash
#
# Copyright 2016-2021 Micro Focus or one of its affiliates.
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


echo "Installing Job Service database."

./install_job_service_db.sh \
  -db.user $POSTGRES_USER \
  -db.pass $POSTGRES_PASSWORD \
  -db.name ${POSTGRES_DB:-jobservice} \
  -db.connection jdbc:postgresql://127.0.0.1:5432/
echo "Completed installation of Job Service database."
