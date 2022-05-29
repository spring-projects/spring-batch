ALTER TABLE BATCH_STEP_EXECUTION ADD CREATE_TIME TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:00';
-- ALTER TABLE BATCH_STEP_EXECUTION ALTER COLUMN START_TIME DROP NOT NULL;
-- ALTER COLUMN is not supported in SQLITE: https://www.sqlite.org/lang_altertable.html
-- There are several ways to drop the 'NOT NULL' constraint on START_TIME, this is left to the user.