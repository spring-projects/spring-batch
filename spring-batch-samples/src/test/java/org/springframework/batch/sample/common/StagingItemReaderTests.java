package org.springframework.batch.sample.common;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class StagingItemReaderTests {

	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;
	
	@Autowired
	private StagingItemWriter<String> writer;

	@Autowired
	private StagingItemReader<String> reader;

	private Long jobId = 11L;


	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Before
	public void onSetUpBeforeTransaction() throws Exception {
		StepExecution stepExecution = new StepExecution("stepName", new JobExecution(new JobInstance(jobId,
				new JobParameters(), "testJob")));
		reader.beforeStep(stepExecution);
		writer.beforeStep(stepExecution);
		writer.write(Arrays.asList(new String[] {"FOO","BAR","SPAM","BUCKET"}));
		reader.open(new ExecutionContext());
	}

	@After
	public void onTearDownAfterTransaction() throws Exception {
		reader.close(null);
		simpleJdbcTemplate.update("DELETE FROM BATCH_STAGING");
	}

	@Transactional @Test
	public void testReaderUpdatesProcessIndicator() throws Exception {

		long id = simpleJdbcTemplate.queryForLong("SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?",
				jobId);
		String before = simpleJdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
				String.class, id);
		assertEquals(StagingItemWriter.NEW, before);

		String item = reader.read();
		assertEquals("FOO", item);

		String after = simpleJdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
				String.class, id);
		assertEquals(StagingItemWriter.DONE, after);

	}

	@Transactional @Test
	public void testUpdateProcessIndicatorAfterCommit() throws Exception {
		testReaderUpdatesProcessIndicator();
		TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
		txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		txTemplate.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus transactionStatus) {
						long id = simpleJdbcTemplate.queryForLong("SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?",
								jobId);
						String before =
								simpleJdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
								String.class, id);
						assertEquals(StagingItemWriter.DONE, before);
						return null;
					}
				});
	}

	@Transactional @Test
	public void testProviderRollsBackMultipleTimes() throws Exception {

		TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
		txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		reader.mark();

		txTemplate.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus transactionStatus) {
						int count = simpleJdbcTemplate.queryForInt("SELECT COUNT(*) from BATCH_STAGING where JOB_ID=? AND PROCESSED=?",
								jobId, StagingItemWriter.NEW);
						assertEquals(4, count);

						Object item = reader.read();
						assertEquals("FOO", item);
						item = reader.read();
						assertEquals("BAR", item);

						transactionStatus.setRollbackOnly();

						return null;
					}
				});

		reader.reset();

		txTemplate.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus transactionStatus) {
						Object item = reader.read();
						assertEquals("FOO", item);
						item = reader.read();
						assertEquals("BAR", item);
						item = reader.read();
						assertEquals("SPAM", item);

						transactionStatus.setRollbackOnly();

						return null;
					}
				});

		reader.reset();

		txTemplate.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus transactionStatus) {
						Object item = reader.read();
						assertEquals("FOO", item);

						transactionStatus.setRollbackOnly();

						return null;
					}
				});

	}

	@Transactional @Test
	public void testProviderRollsBackProcessIndicator() throws Exception {

		TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
		txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		reader.mark();
		// After a rollback we have to resynchronize the TX to simulate a real
		// batch
		final Long idToUse = (Long)txTemplate.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus transactionStatus) {

						long id = simpleJdbcTemplate.queryForLong("SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?",
								jobId);
						String before = simpleJdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
								String.class, id);
						assertEquals(StagingItemWriter.NEW, before);

						Object item = reader.read();
						assertEquals("FOO", item);

						transactionStatus.setRollbackOnly();

						return id;
					}
				});

		reader.reset();

		// After a rollback we have to resynchronize the TX to simulate a real
		// batch
		txTemplate.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus transactionStatus) {

						String after = simpleJdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
								String.class, idToUse);
						assertEquals(StagingItemWriter.NEW, after);

						Object item = reader.read();
						assertEquals("FOO", item);

						transactionStatus.setRollbackOnly();

						return null;
					}
				});

	}
}
