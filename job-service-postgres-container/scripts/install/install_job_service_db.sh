#!/bin/bash

echo "Installing Job Service database"

java -jar /job-service-db.jar $@

echo "Installed Job Service database"
