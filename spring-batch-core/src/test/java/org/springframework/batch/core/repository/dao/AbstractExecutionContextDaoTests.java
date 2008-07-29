package org.springframework.batch.core.repository.dao;

import java.util.HashMap;

import static org.junit.Assert.*;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;

public abstract class AbstractExecutionContextDaoTests {
	
	private ExecutionContextDao dao;
	
	private JobExecutionDao jobDao;
	
	private JobExecution execution;

	public void testSaveAndFindContext() {
		jobDao.saveJobExecution(execution);
		ExecutionContext ctx = new ExecutionContext(new HashMap<String, Object>() {
			{
				put("key", "value");
			}
		});
		execution.setExecutionContext(ctx);
		dao.persistExecutionContext(execution);

		ExecutionContext retrieved = dao.getExecutionContext(execution);
		assertEquals(ctx, retrieved);
	}

	public void testSaveAndFindEmptyContext() {
		jobDao.saveJobExecution(execution);
		ExecutionContext ctx = new ExecutionContext();
		execution.setExecutionContext(ctx);
		dao.persistExecutionContext(execution);

		ExecutionContext retrieved = dao.getExecutionContext(execution);
		assertEquals(ctx, retrieved);
	}

	public void testUpdateContext() {
		jobDao.saveJobExecution(execution);
		ExecutionContext ctx = new ExecutionContext(new HashMap<String, Object>() {
			{
				put("key", "value");
			}
		});
		execution.setExecutionContext(ctx);
		dao.persistExecutionContext(execution);

		ctx.putLong("longKey", 7);
		dao.persistExecutionContext(execution);

		ExecutionContext retrieved = dao.getExecutionContext(execution);
		assertEquals(ctx, retrieved);
		assertEquals(7, retrieved.getLong("longKey"));
	}
}
