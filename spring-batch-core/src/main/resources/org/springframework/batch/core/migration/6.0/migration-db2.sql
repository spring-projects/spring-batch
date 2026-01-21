-- The following is only applicable to Informix servers.
-- IBM DB2 LUW (10.1+) does not support sequence renaming: https://www.ibm.com/docs/en/db2/12.1.x?topic=statements-rename
-- To address this limitation, use a stored procedure or a Java-based program that retrieves
-- the last sequence number and creates a new sequence starting from that value.
RENAME SEQUENCE BATCH_JOB_SEQ TO BATCH_JOB_INSTANCE_SEQ;
