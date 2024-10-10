// to execute in MongoShell after changing the database name `db.` as needed
db.createCollection("BATCH_JOB_INSTANCE");
db.createCollection("BATCH_JOB_EXECUTION");
db.createCollection("BATCH_STEP_EXECUTION");
db.createCollection("BATCH_JOB_INSTANCE_SEQ");
db.createCollection("BATCH_JOB_EXECUTION_SEQ");
db.createCollection("BATCH_STEP_EXECUTION_SEQ");
db.getCollection("BATCH_JOB_INSTANCE_SEQ").insertOne({count : 0});
db.getCollection("BATCH_JOB_EXECUTION_SEQ").insertOne({count : 0});
db.getCollection("BATCH_STEP_EXECUTION_SEQ").insertOne({count : 0});
