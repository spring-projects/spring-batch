/*
 * Copyright 2006-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.sample.domain.mail.internal.TestMailErrorHandler;
import org.springframework.batch.sample.domain.mail.internal.TestMailSender;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dan Garrette
 * @author Dave Syer
 *
 * @Since 2.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/mailJob.xml", "/job-runner-context.xml" })
public class MailJobFunctionalTests {

	private static final String email = "to@company.com";

	private static final Object[] USER1 = new Object[] { 1, "George Washington", email };

	private static final Object[] USER2_SKIP = new Object[] { 2, "John Adams", "FAILURE" };

	private static final Object[] USER3 = new Object[] { 3, "Thomas Jefferson", email };

	private static final Object[] USER4_SKIP = new Object[] { 4, "James Madison", "FAILURE" };

	private static final Object[] USER5 = new Object[] { 5, "James Monroe", email };

	private static final Object[] USER6 = new Object[] { 6, "John Quincy Adams", email };

	private static final Object[] USER7 = new Object[] { 7, "Andrew Jackson", email };

	private static final Object[] USER8 = new Object[] { 8, "Martin Van Buren", email };

	private JdbcOperations jdbcTemplate;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private TestMailErrorHandler errorHandler;

	@Autowired
	private TestMailSender mailSender;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Before
	public void before() {
		mailSender.clear();
		errorHandler.clear();
		jdbcTemplate.update("create table USERS (ID INTEGER, NAME VARCHAR(40), EMAIL VARCHAR(20))");
	}

	@After
	public void after() throws Exception {
		jdbcTemplate.update("drop table USERS");
	}

	@Test
	public void testSkip() throws Exception {
		this.createUsers(new Object[][] { USER1, USER2_SKIP, USER3, USER4_SKIP, USER5, USER6, USER7, USER8 });

		JobExecution jobExecution = jobLauncherTestUtils.launchJob();
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

		List<SimpleMailMessage> receivedMessages = mailSender.getReceivedMessages();
		assertEquals(6, receivedMessages.size());
		Iterator<SimpleMailMessage> emailIter = receivedMessages.iterator();
		for (Object[] record : new Object[][] { USER1, USER3, USER5, USER6, USER7, USER8 }) {
			SimpleMailMessage email = emailIter.next();
			assertEquals("Hello " + record[1], email.getText());
		}

		assertEquals(2, this.errorHandler.getFailedMessages().size());
		Iterator<MailMessage> failureItr = this.errorHandler.getFailedMessages().iterator();
		for (Object[] record : new Object[][] { USER2_SKIP, USER4_SKIP }) {
			SimpleMailMessage email = (SimpleMailMessage) failureItr.next();
			assertEquals("Hello " + record[1], email.getText());
		}
	}

	private void createUsers(Object[][] records) {
		for (Object[] record : records) {
			jdbcTemplate.update("insert into USERS values (?,?,?)", record);
		}
	}


}
