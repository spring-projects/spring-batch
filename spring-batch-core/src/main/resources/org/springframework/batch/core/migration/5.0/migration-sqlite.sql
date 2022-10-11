ALTER TABLE BATCH_STEP_EXECUTION ADD CREATE_TIME TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:00';
-- ALTER TABLE BATCH_STEP_EXECUTION ALTER COLUMN START_TIME DROP NOT NULL;
-- ALTER COLUMN is not supported in SQLITE: https://www.sqlite.org/lang_altertable.html
-- There are several ways to drop the 'NOT NULL' constraint on START_TIME, this is left to the user.

ALTER TABLE BATCH_JOB_EXECUTION_PARAMS DROP COLUMN DATE_VAL;
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS DROP COLUMN LONG_VAL;
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS DROP COLUMN DOUBLE_VAL;

-- ALTER COLUMN is not supported in SQLITE: https://www.sqlite.org/lang_altertable.html
-- => migration scripts to change column types introduced in Spring Batch v5 are not provided
