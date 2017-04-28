
--insert script that 'copies' existing batch_job_params to batch_job_execution_params
--sets new params to identifying ones
--selects the 'first'(minimum) job_execution_id for a job_instance_id

INSERT INTO BATCH_JOB_EXECUTION_PARAMS (
	JOB_EXECUTION_ID , TYPE_CD , KEY_NAME , STRING_VAL , DATE_VAL , LONG_VAL , DOUBLE_VAL , IDENTIFYING
) SELECT (
	SELECT 
		MIN(JOB_EXECUTION_ID) 
	FROM 
		BATCH_JOB_EXECUTION JE, BATCH_JOB_PARAMS JP 
	WHERE 
		JP.JOB_INSTANCE_ID = JE.JOB_INSTANCE_ID AND JE.JOB_INSTANCE_ID = J.JOB_INSTANCE_ID
		),
	J.TYPE_CD, J.KEY_NAME, J.STRING_VAL, J.DATE_VAL, J.LONG_VAL, J.DOUBLE_VAL, 1 
FROM 
	BATCH_JOB_PARAMS J;	