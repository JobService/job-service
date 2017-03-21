#!/bin/bash

echo "Installing Job Service database."

./install_job_service_db.sh \
  -db.user $POSTGRES_USER \
  -db.pass $POSTGRES_PASSWORD \
  -db.name ${POSTGRES_DB:-jobservice} \
  -db.connection jdbc:postgresql://127.0.0.1:5432

echo "Completed installation of Job Service database."
