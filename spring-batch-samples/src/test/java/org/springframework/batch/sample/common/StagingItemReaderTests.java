package org.springframework.batch.sample.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
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

	private Long jobId = 113L;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@BeforeTransaction
	public void onSetUpBeforeTransaction() throws Exception {
		StepExecution stepExecution = new StepExecution("stepName", new JobExecution(new JobInstance(jobId,
				new JobParameters(), "testJob")));
		writer.beforeStep(stepExecution);
		writer.write(Arrays.asList(new String[] { "FOO", "BAR", "SPAM", "BUCKET" }));
		reader.beforeStep(stepExecution);
	}

	@AfterTransaction
	public void onTearDownAfterTransaction() throws Exception {
		reader.destroy();
		simpleJdbcTemplate.update("DELETE FROM BATCH_STAGING");
	}

	@Transactional
	@Test
	public void testReaderWithProcessorUpdatesProcessIndicator() throws Exception {

		long id = simpleJdbcTemplate.queryForLong("SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?", jobId);
		String before = simpleJdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
				String.class, id);
		assertEquals(StagingItemWriter.NEW, before);

		ProcessIndicatorItemWrapper<String> wrapper = reader.read();
		String item = wrapper.getItem(); 
		assertEquals("FOO", item);
		
		StagingItemProcessor<String> updater = new StagingItemProcessor<String>();
		updater.setJdbcTemplate(simpleJdbcTemplate);
		updater.process(wrapper);

		String after = simpleJdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
				String.class, id);
		assertEquals(StagingItemWriter.DONE, after);

	}

	@Transactional
	@Test
	public void testUpdateProcessIndicatorAfterCommit() throws Exception {
		TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
		txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		txTemplate.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				try {
					testReaderWithProcessorUpdatesProcessIndicator();
				}
				catch (Exception e) {
					fail("Unxpected Exception: " + e);
				}
				return null;
			}
		});
		long id = simpleJdbcTemplate.queryForLong("SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?", jobId);
		String before = simpleJdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
				String.class, id);
		assertEquals(StagingItemWriter.DONE, before);
	}

	@Transactional
	@Test
	public void testReaderRollsBackProcessIndicator() throws Exception {

		TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
		txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		final Long idToUse = (Long) txTemplate.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {

				long id = simpleJdbcTemplate.queryForLong("SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?", jobId);
				String before = simpleJdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
						String.class, id);
				assertEquals(StagingItemWriter.NEW, before);

				ProcessIndicatorItemWrapper<String> wrapper = reader.read();
				assertEquals("FOO", wrapper.getItem());

				transactionStatus.setRollbackOnly();

				return id;
			}
		});

		String after = simpleJdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
				String.class, idToUse);
		assertEquals(StagingItemWriter.NEW, after);

	}
}
