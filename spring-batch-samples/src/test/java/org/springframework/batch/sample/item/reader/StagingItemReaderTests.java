package org.springframework.batch.sample.item.reader;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.sample.item.writer.StagingItemWriter;
import org.springframework.batch.sample.tasklet.JobSupport;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.util.ClassUtils;

public class StagingItemReaderTests extends AbstractTransactionalDataSourceSpringContextTests {

	private StagingItemWriter writer;

	private StagingItemReader reader;

	private Long jobId = new Long(11);

	public void setProcessor(StagingItemWriter writer) {
		this.writer = writer;
	}

	public void setProvider(StagingItemReader reader) {
		this.reader = reader;
	}

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(StagingItemWriter.class,
				"staging-test-context.xml") };
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.test.AbstractTransactionalSpringContextTests#onSetUpBeforeTransaction()
	 */
	protected void onSetUpBeforeTransaction() throws Exception {
		StepExecution stepExecution = new StepExecution("stepName", new JobExecution(new JobInstance(jobId,
				new JobParameters(), new JobSupport("testJob"))));
		reader.beforeStep(stepExecution);
		writer.beforeStep(stepExecution);
	}

	protected void onSetUpInTransaction() throws Exception {
		writer.write("FOO");
		writer.write("BAR");
		writer.write("SPAM");
		writer.write("BUCKET");
		reader.open(new ExecutionContext());
	}

	protected void onTearDownAfterTransaction() throws Exception {
		reader.close(null);
		getJdbcTemplate().update("DELETE FROM BATCH_STAGING");
	}

	public void testReaderUpdatesProcessIndicator() throws Exception {

		long id = getJdbcTemplate().queryForLong("SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?",
				new Object[] { jobId });
		String before = (String) getJdbcTemplate().queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
				new Object[] { new Long(id) }, String.class);
		assertEquals(StagingItemWriter.NEW, before);

		Object item = reader.read();
		assertEquals("FOO", item);

		String after = (String) getJdbcTemplate().queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
				new Object[] { new Long(id) }, String.class);
		assertEquals(StagingItemWriter.DONE, after);

	}

	public void testUpdateProcessIndicatorAfterCommit() throws Exception {
		testReaderUpdatesProcessIndicator();
		setComplete();
		endTransaction();
		startNewTransaction();
		long id = getJdbcTemplate().queryForLong("SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?",
				new Object[] { jobId });
		String before = (String) getJdbcTemplate().queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
				new Object[] { new Long(id) }, String.class);
		assertEquals(StagingItemWriter.DONE, before);
	}

	public void testProviderRollsBackMultipleTimes() throws Exception {

		reader.mark();
		setComplete();
		endTransaction();
		startNewTransaction();

		int count = getJdbcTemplate().queryForInt("SELECT COUNT(*) from BATCH_STAGING where JOB_ID=? AND PROCESSED=?",
				new Object[] { jobId, StagingItemWriter.NEW });
		assertEquals(4, count);

		Object item = reader.read();
		assertEquals("FOO", item);
		item = reader.read();
		assertEquals("BAR", item);

		reader.reset();
		endTransaction();
		startNewTransaction();

		item = reader.read();
		assertEquals("FOO", item);
		item = reader.read();
		assertEquals("BAR", item);
		item = reader.read();
		assertEquals("SPAM", item);

		reader.reset();
		endTransaction();
		startNewTransaction();

		item = reader.read();
		assertEquals("FOO", item);

	}

	public void testProviderRollsBackProcessIndicator() throws Exception {

		reader.mark();
		setComplete();
		endTransaction();
		startNewTransaction();
		// After a rollback we have to resynchronize the TX to simulate a real
		// batch

		long id = getJdbcTemplate().queryForLong("SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?",
				new Object[] { jobId });
		String before = (String) getJdbcTemplate().queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
				new Object[] { new Long(id) }, String.class);
		assertEquals(StagingItemWriter.NEW, before);

		Object item = reader.read();
		assertEquals("FOO", item);

		reader.reset();
		endTransaction();
		startNewTransaction();
		// After a rollback we have to resynchronize the TX to simulate a real
		// batch

		String after = (String) getJdbcTemplate().queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
				new Object[] { new Long(id) }, String.class);
		assertEquals(StagingItemWriter.NEW, after);

		item = reader.read();
		assertEquals("FOO", item);
	}
}
