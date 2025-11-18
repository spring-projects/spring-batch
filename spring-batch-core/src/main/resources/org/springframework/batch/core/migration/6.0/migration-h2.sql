-- Sequence rename is not supported in H2
-- https://github.com/h2database/h2database/issues/417

CREATE SEQUENCE BATCH_JOB_INSTANCE_SEQ;
-- Copy last value from old sequence to new sequence
DROP SEQUENCE BATCH_JOB_SEQ;
