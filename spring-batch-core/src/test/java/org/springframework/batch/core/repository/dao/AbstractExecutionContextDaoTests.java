package org.springframework.batch.core.repository.dao;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for {@link ExecutionContextDao} implementations.
 */
public abstract class AbstractExecutionContextDaoTests extends AbstractTransactionalJUnit4SpringContextTests {

	private ExecutionContextDao dao;

	private JobExecution jobExecution = new JobExecution(new JobInstance(1L, new JobParameters(), "jobName"), 1L);

	private StepExecution stepExecution = new StepExecution("stepName", jobExecution, 1L);

	@Before
	public void setUp() {
		dao = getExecutionContextDao();
	}

	/**
	 * @return Configured {@link ExecutionContextDao} implementation ready for
	 * use.
	 */
	protected abstract ExecutionContextDao getExecutionContextDao();

	@Transactional
	@Test
	public void testSaveAndFindContext() {

		ExecutionContext ctx = new ExecutionContext(Collections.<String, Object> singletonMap("key", "value"));
		jobExecution.setExecutionContext(ctx);
		dao.persistExecutionContext(jobExecution);

		ExecutionContext retrieved = dao.getExecutionContext(jobExecution);
		assertEquals(ctx, retrieved);
	}

	@Transactional
	@Test
	public void testSaveAndFindEmptyContext() {

		ExecutionContext ctx = new ExecutionContext();
		jobExecution.setExecutionContext(ctx);
		dao.persistExecutionContext(jobExecution);

		ExecutionContext retrieved = dao.getExecutionContext(jobExecution);
		assertEquals(ctx, retrieved);
	}

	@Transactional
	@Test
	public void testUpdateContext() {

		ExecutionContext ctx = new ExecutionContext(Collections
				.<String, Object> singletonMap("key", "value"));
		jobExecution.setExecutionContext(ctx);
		dao.persistExecutionContext(jobExecution);

		ctx.putLong("longKey", 7);
		dao.persistExecutionContext(jobExecution);

		ExecutionContext retrieved = dao.getExecutionContext(jobExecution);
		assertEquals(ctx, retrieved);
		assertEquals(7, retrieved.getLong("longKey"));
	}

	@Transactional
	@Test
	public void testSaveAndFindStepContext() {

		ExecutionContext ctx = new ExecutionContext(Collections.<String, Object> singletonMap("key", "value"));
		stepExecution.setExecutionContext(ctx);
		dao.persistExecutionContext(stepExecution);

		ExecutionContext retrieved = dao.getExecutionContext(stepExecution);
		assertEquals(ctx, retrieved);
	}

	@Transactional
	@Test
	public void testSaveAndFindEmptyStepContext() {

		ExecutionContext ctx = new ExecutionContext();
		stepExecution.setExecutionContext(ctx);
		dao.persistExecutionContext(stepExecution);

		ExecutionContext retrieved = dao.getExecutionContext(stepExecution);
		assertEquals(ctx, retrieved);
	}

	@Transactional
	@Test
	public void testUpdateStepContext() {

		ExecutionContext ctx = new ExecutionContext(Collections.<String, Object> singletonMap("key", "value"));
		stepExecution.setExecutionContext(ctx);
		dao.persistExecutionContext(stepExecution);

		ctx.putLong("longKey", 7);
		dao.persistExecutionContext(stepExecution);

		ExecutionContext retrieved = dao.getExecutionContext(stepExecution);
		assertEquals(ctx, retrieved);
		assertEquals(7, retrieved.getLong("longKey"));
	}

	@Transactional
	@Test
	public void testStoreInteger() {

		ExecutionContext ec = new ExecutionContext();
		ec.put("intValue", new Integer(343232));
		stepExecution.setExecutionContext(ec);
		dao.persistExecutionContext(stepExecution);
		ExecutionContext restoredEc = dao.getExecutionContext(stepExecution);
		assertEquals(ec, restoredEc);
	}

}
