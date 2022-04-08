--
-- Copyright 2016-2021 Micro Focus or one of its affiliates.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--


INSERT INTO default_job_expiration_policy(job_status, operation, expiration_time, create_date_offset, last_modified_offset)
VALUES ('Failed', 'Delete', 'infinity'::timestamp, '', null);

INSERT INTO default_job_expiration_policy(job_status, operation, expiration_time, create_date_offset, last_modified_offset)
VALUES ('Cancelled', 'Delete', 'infinity'::timestamp, '', null);

INSERT INTO default_job_expiration_policy(job_status, operation, expiration_time, create_date_offset, last_modified_offset)
VALUES ('Expired', 'Delete', 'infinity'::timestamp, '', null);

INSERT INTO default_job_expiration_policy(job_status, operation, expiration_time, create_date_offset, last_modified_offset)
VALUES ('Paused', 'Delete', 'infinity'::timestamp, '', null);

INSERT INTO default_job_expiration_policy(job_status, operation, expiration_time, create_date_offset, last_modified_offset)
VALUES ('Active', 'Delete', 'infinity'::timestamp, '', null);

INSERT INTO default_job_expiration_policy(job_status, operation, expiration_time, create_date_offset, last_modified_offset)
VALUES ('Waiting', 'Delete', 'infinity'::timestamp, '', null);

INSERT INTO default_job_expiration_policy(job_status, operation, expiration_time, create_date_offset, last_modified_offset)
VALUES ('Completed', 'Delete', 'infinity'::timestamp, '', null);
