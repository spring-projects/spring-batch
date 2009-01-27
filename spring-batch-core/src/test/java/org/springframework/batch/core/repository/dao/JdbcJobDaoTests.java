package org.springframework.batch.core.repository.dao;

import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import org.springframework.batch.core.ExitStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"sql-dao-test.xml"})
public class JdbcJobDaoTests extends AbstractJobDaoTests {

	public static final String LONG_STRING = "A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String ";

	@Before
	public void onSetUpBeforeTransaction() throws Exception {
		((JdbcJobInstanceDao) jobInstanceDao).setTablePrefix(AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX);
		((JdbcJobExecutionDao) jobExecutionDao).setTablePrefix(AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX);
	}

	@Transactional @Test
	public void testUpdateJobExecutionWithLongExitCode() {

		assertTrue(LONG_STRING.length() > 250);
		((JdbcJobExecutionDao) jobExecutionDao).setExitMessageLength(250);
		jobExecution.setExitStatus(ExitStatus.COMPLETED
				.addExitDescription(LONG_STRING));
		jobExecutionDao.updateJobExecution(jobExecution);

		List<Map<String, Object>> executions = simpleJdbcTemplate.queryForList(
				"SELECT * FROM BATCH_JOB_EXECUTION where JOB_INSTANCE_ID=?",
				jobInstance.getId());
		assertEquals(1, executions.size());
		assertEquals(LONG_STRING.substring(0, 250), executions.get(0)
				.get("EXIT_MESSAGE"));
	}

}
