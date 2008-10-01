package org.springframework.batch.core.repository.dao;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

public class MapExecutionContextDao implements ExecutionContextDao {

	private static Map<Long, ExecutionContext> contextsByStepExecutionId = new HashMap<Long, ExecutionContext>();

	private static Map<Long, ExecutionContext> contextsByJobExecutionId = new HashMap<Long, ExecutionContext>();
	
	public static void clear() {
		contextsByJobExecutionId.clear();
		contextsByStepExecutionId.clear();
	}


	public ExecutionContext getExecutionContext(StepExecution stepExecution) {
		return contextsByStepExecutionId.get(stepExecution.getId());
	}

	public void persistExecutionContext(StepExecution stepExecution) {
		contextsByStepExecutionId.put(stepExecution.getId(), stepExecution.getExecutionContext());
	}
	
	public ExecutionContext getExecutionContext(JobExecution jobExecution) {
		return contextsByJobExecutionId.get(jobExecution.getId());
	}

	public void persistExecutionContext(JobExecution jobExecution) {
		contextsByJobExecutionId.put(jobExecution.getId(), jobExecution.getExecutionContext());

	}

}
