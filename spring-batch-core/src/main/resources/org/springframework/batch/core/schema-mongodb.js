// to execute in MongoShell after changing the database name `db.` as needed
// This script is idempotent and can be safely reapplied.

// SEQUENCES
db.getCollection("BATCH_SEQUENCES").updateOne(
    {_id: "BATCH_JOB_INSTANCE_SEQ"},
    {$setOnInsert: {count: NumberLong(0)}},
    {upsert: true}
);
db.getCollection("BATCH_SEQUENCES").updateOne(
    {_id: "BATCH_JOB_EXECUTION_SEQ"},
    {$setOnInsert: {count: NumberLong(0)}},
    {upsert: true}
);
db.getCollection("BATCH_SEQUENCES").updateOne(
    {_id: "BATCH_STEP_EXECUTION_SEQ"},
    {$setOnInsert: {count: NumberLong(0)}},
    {upsert: true}
);

// INDICES
db.getCollection("BATCH_JOB_INSTANCE").createIndex( {"jobName": 1}, {"name": "job_name_idx"});
db.getCollection("BATCH_JOB_INSTANCE").createIndex( {"jobName": 1, "jobKey": 1}, {"name": "job_name_key_idx"});
db.getCollection("BATCH_JOB_INSTANCE").createIndex( {"jobInstanceId": -1}, {"name": "job_instance_idx"});
db.getCollection("BATCH_JOB_EXECUTION").createIndex( {"jobInstanceId": 1}, {"name": "job_instance_idx"});
db.getCollection("BATCH_JOB_EXECUTION").createIndex( {"jobInstanceId": 1, "status": 1}, {"name": "job_instance_status_idx"});
db.getCollection("BATCH_STEP_EXECUTION").createIndex( {"stepExecutionId": 1}, {"name": "step_execution_idx"});
