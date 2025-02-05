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
db.getCollection("BATCH_JOB_INSTANCE").createIndex( {"jobName": 1}, {"name": "job_name_idx"});
db.getCollection("BATCH_JOB_INSTANCE").createIndex( {"jobName": 1, "jobKey": 1}, {"name": "job_name_key_idx"});
db.getCollection("BATCH_JOB_INSTANCE").createIndex( {"jobInstanceId": -1}, {"name": "job_instance_idx"});
db.getCollection("BATCH_JOB_EXECUTION").createIndex( {"jobInstanceId": 1}, {"name": "job_instance_idx"});
db.getCollection("BATCH_JOB_EXECUTION").createIndex( {"jobInstanceId": 1, "status": 1}, {"name": "job_instance_status_idx"});
db.getCollection("BATCH_STEP_EXECUTION").createIndex( {"stepExecutionId": 1}, {"name": "step_execution_idx"});
