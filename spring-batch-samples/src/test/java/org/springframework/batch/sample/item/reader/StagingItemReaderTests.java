package org.springframework.batch.sample.item.reader;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobInstanceProperties;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.execution.scope.SimpleStepContext;
import org.springframework.batch.execution.scope.StepSynchronizationManager;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.synch.BatchTransactionSynchronizationManager;
import org.springframework.batch.repeat.synch.RepeatSynchronizationManager;
import org.springframework.batch.sample.item.processor.StagingItemProcessor;
import org.springframework.batch.sample.item.reader.StagingItemReader;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.util.ClassUtils;

public class StagingItemReaderTests extends
		AbstractTransactionalDataSourceSpringContextTests {

	private StagingItemProcessor processor;
	private StagingItemReader provider;
	private Long jobId;

	public void setProcessor(StagingItemProcessor processor) {
		this.processor = processor;
	}

	public void setProvider(StagingItemReader provider) {
		this.provider = provider;
	}

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(
				StagingItemProcessor.class, "staging-test-context.xml") };
	}

	protected void prepareTestInstance() throws Exception {
		SimpleStepContext stepScopeContext = StepSynchronizationManager.open();
		jobId = new Long(11);
		stepScopeContext.setStepExecution(new StepExecution(new StepInstance(
				new Long(12)), new JobExecution(new JobInstance(jobId,
				new JobInstanceProperties()))));
		RepeatSynchronizationManager.register(new RepeatContextSupport(null));
		super.prepareTestInstance();
	}

	protected void onSetUpInTransaction() throws Exception {
		processor.process("FOO");
		processor.process("BAR");
		processor.process("SPAM");
		processor.process("BUCKET");
	}

	protected void onTearDownAfterTransaction() throws Exception {
		provider.close();
		getJdbcTemplate().update("DELETE FROM BATCH_STAGING");
	}

	public void testReaderUpdatesProcessIndicator() throws Exception {

		long id = getJdbcTemplate().queryForLong(
				"SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?",
				new Object[] { jobId });
		String before = (String) getJdbcTemplate().queryForObject(
				"SELECT PROCESSED from BATCH_STAGING where ID=?",
				new Object[] { new Long(id) }, String.class);
		assertEquals(StagingItemProcessor.NEW, before);

		Object item = provider.read();
		assertEquals("FOO", item);

		String after = (String) getJdbcTemplate().queryForObject(
				"SELECT PROCESSED from BATCH_STAGING where ID=?",
				new Object[] { new Long(id) }, String.class);
		assertEquals(StagingItemProcessor.DONE, after);

	}
	
	public void testUpdateProcessIndicatorAfterCommit() throws Exception {
		testReaderUpdatesProcessIndicator();
		setComplete();
		endTransaction();
		startNewTransaction();
		long id = getJdbcTemplate().queryForLong(
				"SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?",
				new Object[] { jobId });
		String before = (String) getJdbcTemplate().queryForObject(
				"SELECT PROCESSED from BATCH_STAGING where ID=?",
				new Object[] { new Long(id) }, String.class);
		assertEquals(StagingItemProcessor.DONE, before);	
	}

	public void testProviderRollsBackMultipleTimes() throws Exception {

		setComplete();
		endTransaction();
		startNewTransaction();
		// After a rollback we have to resynchronize the TX to simulate a real batch
		BatchTransactionSynchronizationManager.resynchronize();

		int count = getJdbcTemplate().queryForInt(
				"SELECT COUNT(*) from BATCH_STAGING where JOB_ID=? AND PROCESSED=?",
				new Object[] { jobId, StagingItemProcessor.NEW });
		assertEquals(4, count);
		
		Object item = provider.read();
		assertEquals("FOO", item);
		item = provider.read();
		assertEquals("BAR", item);

		endTransaction();
		startNewTransaction();
		BatchTransactionSynchronizationManager.resynchronize();
		
		item = provider.read();
		assertEquals("FOO", item);
		item = provider.read();
		assertEquals("BAR", item);
		item = provider.read();
		assertEquals("SPAM", item);

		endTransaction();
		startNewTransaction();
		BatchTransactionSynchronizationManager.resynchronize();
		
		item = provider.read();
		assertEquals("FOO", item);

	}
	
	public void testProviderRollsBackProcessIndicator() throws Exception {

		setComplete();
		endTransaction();
		startNewTransaction();
		// After a rollback we have to resynchronize the TX to simulate a real batch
		BatchTransactionSynchronizationManager.resynchronize();

		long id = getJdbcTemplate().queryForLong(
				"SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?",
				new Object[] { jobId });
		String before = (String) getJdbcTemplate().queryForObject(
				"SELECT PROCESSED from BATCH_STAGING where ID=?",
				new Object[] { new Long(id) }, String.class);
		assertEquals(StagingItemProcessor.NEW, before);

		Object item = provider.read();
		assertEquals("FOO", item);

		endTransaction();
		startNewTransaction();
		// After a rollback we have to resynchronize the TX to simulate a real batch
		BatchTransactionSynchronizationManager.resynchronize();

		String after = (String) getJdbcTemplate().queryForObject(
				"SELECT PROCESSED from BATCH_STAGING where ID=?",
				new Object[] { new Long(id) }, String.class);
		assertEquals(StagingItemProcessor.NEW, after);

		item = provider.read();
		assertEquals("FOO", item);
	}
}
