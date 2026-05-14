--
-- Copyright 2016-2026 Open Text.
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

/*
 **************************************************************
 ** Add retry tracking columns to delete_log and create the   **
 ** table_cleanup_failed dead-letter table.                   **
 **                                                           **
 ** delete_log table now records how many times a             **
 ** deletion has been attempted, the last error encountered,  **
 ** and when it was last tried. Entries that exceed the       **
 ** maximum retry threshold are promoted to                   **
 ** table_cleanup_failed so that operators can inspect and    **
 ** remediate them without blocking ongoing cleanup work.     **
 **                                                           **
 ** table_cleanup_failed is also used in case of any transient**
 ** failures during population of delete_log entries.         **
 **************************************************************
 */

-- Add retry tracking columns to the existing delete_log table.
ALTER TABLE public.delete_log
    ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE public.delete_log
    ADD COLUMN IF NOT EXISTS last_error TEXT;

ALTER TABLE public.delete_log
    ADD COLUMN IF NOT EXISTS last_attempted_at TIMESTAMPTZ;

-- Dead-letter table for entries fails during cleanup procedure.
CREATE TABLE IF NOT EXISTS public.table_cleanup_failed
(
    table_name        VARCHAR(63)  NOT NULL,
    last_error        TEXT,
    first_failed_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_attempted_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT table_cleanup_failed_pkey PRIMARY KEY (table_name)
);
