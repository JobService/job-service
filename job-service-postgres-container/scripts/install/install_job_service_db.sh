#!/bin/bash

echo "installing job service database"

java \
  -jar /job-service-db.jar $@
  
echo "installed job service database"
