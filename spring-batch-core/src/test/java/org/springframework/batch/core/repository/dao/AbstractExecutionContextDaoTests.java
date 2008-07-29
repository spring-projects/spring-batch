package org.springframework.batch.core.repository.dao;

import java.util.HashMap;

import static org.junit.Assert.*;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

public abstract class AbstractExecutionContextDaoTests {
	
	private ExecutionContextDao dao;
	
	private JobExecutionDao jobDao;
	
	private StepExecutionDao stepDao;
	
	private JobExecution execution;
	
	private StepExecution stepExecution;

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
	
	public void testSaveAndFindStepContext() {
		stepDao.saveStepExecution(stepExecution);
		ExecutionContext ctx = new ExecutionContext(new HashMap<String, Object>() {
			{
				put("key", "value");
			}
		});
		stepExecution.setExecutionContext(ctx);
		dao.persistExecutionContext(stepExecution);

		ExecutionContext retrieved = dao.getExecutionContext(stepExecution);
		assertEquals(ctx, retrieved);
	}
	
	public void testSaveAndFindEmptyStepContext() {
		stepDao.saveStepExecution(stepExecution);
		ExecutionContext ctx = new ExecutionContext();
		stepExecution.setExecutionContext(ctx);
		dao.persistExecutionContext(stepExecution);

		ExecutionContext retrieved = dao.getExecutionContext(stepExecution);
		assertEquals(ctx, retrieved);
	}

	public void testUpdateStepContext() {
		stepDao.saveStepExecution(stepExecution);
		ExecutionContext ctx = new ExecutionContext(new HashMap<String, Object>() {
			{
				put("key", "value");
			}
		});
		stepExecution.setExecutionContext(ctx);
		dao.persistExecutionContext(stepExecution);

		ctx.putLong("longKey", 7);
		dao.persistExecutionContext(stepExecution);

		ExecutionContext retrieved = dao.getExecutionContext(stepExecution);
		assertEquals(ctx, retrieved);
		assertEquals(7, retrieved.getLong("longKey"));
	}
	
	public void testStoreInteger(){	
		stepDao.saveStepExecution(stepExecution);
		ExecutionContext ec = new ExecutionContext();
		ec.put("intValue", new Integer(343232));
		stepExecution.setExecutionContext(ec);
		dao.persistExecutionContext(stepExecution);
		ExecutionContext restoredEc = dao.getExecutionContext(stepExecution);
		assertEquals(ec, restoredEc);
	}

}
