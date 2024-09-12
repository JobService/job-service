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

defaultCrashDumpFilePath="/tmp/"

####################################################
# Sets the CRASH_DUMP_FILE_PATH to a default value if it is not set
####################################################
function set_default_crash_dump_file_path_if_not_set() {
  if [ -z "$CRASH_DUMP_FILE_PATH" ]
  then
    echo "CRASH_DUMP_FILE_PATH was not set - using default: $defaultCrashDumpFilePath"
    export CRASH_DUMP_FILE_PATH=$defaultCrashDumpFilePath
    echo "Updated CRASH_DUMP_FILE_PATH: $CRASH_DUMP_FILE_PATH"
  fi

  mkdir -vp "$CRASH_DUMP_FILE_PATH"
}

# If CRASH_DUMP_ON_OUT_OF_MEMORY_ERROR is true, then add JVM argument and append JOB_SERVICE_JAVA_OPTS
if [ "$CRASH_DUMP_ON_OUT_OF_MEMORY_ERROR" == "true" ]
then
  set_default_crash_dump_file_path_if_not_set

  JOB_SERVICE_JAVA_OPTS="${JOB_SERVICE_JAVA_OPTS} -XX:+CrashOnOutOfMemoryError -XX:ErrorFile=${CRASH_DUMP_FILE_PATH}${HOSTNAME}_crash_$(date -u '+%Y%m%dT%H%M%SZ').log"
  echo "CRASH_DUMP_ON_OUT_OF_MEMORY_ERROR set: Updated JOB_SERVICE_JAVA_OPTS: $JOB_SERVICE_JAVA_OPTS"
fi

# If HEAP_DUMP_ON_OUT_OF_MEMORY_ERROR is true, then add JVM argument and append JOB_SERVICE_JAVA_OPTS
if [ "$HEAP_DUMP_ON_OUT_OF_MEMORY_ERROR" == "true" ]
then
  set_default_crash_dump_file_path_if_not_set

  JOB_SERVICE_JAVA_OPTS="${JOB_SERVICE_JAVA_OPTS} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${CRASH_DUMP_FILE_PATH}${HOSTNAME}_heap_dump_$(date -u '+%Y%m%dT%H%M%SZ').hprof"
  echo "HEAP_DUMP_ON_OUT_OF_MEMORY_ERROR set: Updated JOB_SERVICE_JAVA_OPTS: $JOB_SERVICE_JAVA_OPTS"
fi

cd /maven
exec java \
    ${JOB_SERVICE_JAVA_OPTS} \
    -classpath *:classpath \
    com.hpe.caf.services.job.dropwizard.JobServiceApplication
