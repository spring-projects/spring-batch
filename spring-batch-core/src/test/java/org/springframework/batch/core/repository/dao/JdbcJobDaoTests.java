/*
 * Copyright 2008-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.repository.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
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

		List<Map<String, Object>> executions = jdbcTemplate.queryForList(
				"SELECT * FROM BATCH_JOB_EXECUTION where JOB_INSTANCE_ID=?",
				jobInstance.getId());
		assertEquals(1, executions.size());
		assertEquals(LONG_STRING.substring(0, 250), executions.get(0)
				.get("EXIT_MESSAGE"));
	}

}
