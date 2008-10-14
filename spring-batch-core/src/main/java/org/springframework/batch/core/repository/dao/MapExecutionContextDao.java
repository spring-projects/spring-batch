package org.springframework.batch.core.repository.dao;

import java.util.Map;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;

public class MapExecutionContextDao implements ExecutionContextDao {

	private static Map<Long, ExecutionContext> contextsByStepExecutionId = TransactionAwareProxyFactory.createTransactionalMap();

	private static Map<Long, ExecutionContext> contextsByJobExecutionId = TransactionAwareProxyFactory.createTransactionalMap();
	
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
