// to execute in MongoShell after changing the database name `db.` as needed
db.createCollection("BATCH_JOB_INSTANCE");
db.createCollection("BATCH_JOB_EXECUTION");
db.createCollection("BATCH_STEP_EXECUTION");

// SEQUENCES
db.createCollection("BATCH_SEQUENCES");
db.getCollection("BATCH_SEQUENCES").insertOne({_id: "BATCH_JOB_INSTANCE_SEQ", count: Long(0)});
db.getCollection("BATCH_SEQUENCES").insertOne({_id: "BATCH_JOB_EXECUTION_SEQ", count: Long(0)});
db.getCollection("BATCH_SEQUENCES").insertOne({_id: "BATCH_STEP_EXECUTION_SEQ", count: Long(0)});

// INDICES
db.getCollection("BATCH_JOB_INSTANCE").createIndex("job_name_idx", {"jobName": 1}, {});
db.getCollection("BATCH_JOB_INSTANCE").createIndex("job_name_key_idx", {"jobName": 1, "jobKey": 1}, {});
db.getCollection("BATCH_JOB_INSTANCE").createIndex("job_instance_idx", {"jobInstanceId": -1}, {});
db.getCollection("BATCH_JOB_EXECUTION").createIndex("job_instance_idx", {"jobInstanceId": 1}, {});
db.getCollection("BATCH_JOB_EXECUTION").createIndex("job_instance_idx", {"jobInstanceId": 1, "status": 1}, {});
db.getCollection("BATCH_STEP_EXECUTION").createIndex("step_execution_idx", {"stepExecutionId": 1}, {});
