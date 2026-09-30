/*
 * Copyright 2008-present the original author or authors.
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
package org.springframework.batch.core.repository.dao.jdbc;

import org.springframework.batch.infrastructure.support.DatabaseType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * @author Parikshit Dutta
 * @author Mahmoud Ben Hassine
 */
public class JdbcJobExecutionDaoTests {

	private JdbcJobExecutionDao jdbcJobExecutionDao;

	private JdbcJobInstanceDao jdbcJobInstanceDao;

	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setup() throws Exception {
		EmbeddedDatabase database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
			.addScript(DatabaseType.H2.getProductSchemaDrop())
			.addScript(DatabaseType.H2.getProductSchema())
			.build();
		jdbcTemplate = new JdbcTemplate(database);

		jdbcJobInstanceDao = new JdbcJobInstanceDao();
		jdbcJobInstanceDao.setJdbcTemplate(jdbcTemplate);
		H2SequenceMaxValueIncrementer jobInstanceIncrementer = new H2SequenceMaxValueIncrementer(database,
				"BATCH_JOB_INSTANCE_SEQ");
		jdbcJobInstanceDao.setJobInstanceIncrementer(jobInstanceIncrementer);
		jdbcJobInstanceDao.afterPropertiesSet();

		jdbcJobExecutionDao = new JdbcJobExecutionDao();
		jdbcJobExecutionDao.setJdbcTemplate(jdbcTemplate);
		H2SequenceMaxValueIncrementer jobExecutionIncrementer = new H2SequenceMaxValueIncrementer(database,
				"BATCH_JOB_EXECUTION_SEQ");
		jdbcJobExecutionDao.setJobExecutionIncrementer(jobExecutionIncrementer);
		jdbcJobExecutionDao.setJobInstanceDao(jdbcJobInstanceDao);
		jdbcJobExecutionDao.afterPropertiesSet();

	}

	@Test
	void testCreateJobExecution() {
		// given
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);

		// when
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);

		// then
		Assertions.assertNotNull(jobExecution);
		Assertions.assertEquals(1, jobExecution.getId());
		Assertions.assertEquals(jobInstance, jobExecution.getJobInstance());
		int batchJobExecutionsCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION");
		Assertions.assertEquals(1, batchJobExecutionsCount);
	}

	@Test
	void testDeleteJobExecution() {
		// given
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);

		// when
		jdbcJobExecutionDao.deleteJobExecution(jobExecution);

		// then
		Assertions.assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
	}

	@Test
	void testDeleteJobExecutionParameters() {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo").toJobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);

		// when
		jdbcJobExecutionDao.deleteJobExecutionParameters(jobExecution);

		// then
		Assertions.assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION_PARAMS"));
	}

	@Test
	void testJobParametersPersistenceRoundTrip() {
		// given
		Date dateParameter = new Date();
		LocalDate localDateParameter = LocalDate.now();
		LocalTime localTimeParameter = LocalTime.now();
		LocalDateTime localDateTimeParameter = LocalDateTime.now();
		String stringParameter = "foo";
		long longParameter = 1L;
		double doubleParameter = 2D;
		JobParameters jobParameters = new JobParametersBuilder().addString("string", stringParameter)
			.addLong("long", longParameter)
			.addDouble("double", doubleParameter)
			.addDate("date", dateParameter)
			.addLocalDate("localDate", localDateParameter)
			.addLocalTime("localTime", localTimeParameter)
			.addLocalDateTime("localDateTime", localDateTimeParameter)
			.toJobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);

		// when
		JobExecution retrieved = jdbcJobExecutionDao.getJobExecution(jobExecution.getId());

		// then
		JobParameters parameters = retrieved.getJobParameters();
		Assertions.assertNotNull(parameters);
		Assertions.assertEquals(dateParameter, parameters.getDate("date"));
		Assertions.assertEquals(localDateParameter, parameters.getLocalDate("localDate"));
		Assertions.assertEquals(localTimeParameter, parameters.getLocalTime("localTime"));
		Assertions.assertEquals(localDateTimeParameter, parameters.getLocalDateTime("localDateTime"));
		Assertions.assertEquals(stringParameter, parameters.getString("string"));
		Assertions.assertEquals(longParameter, parameters.getLong("long"));
		Assertions.assertEquals(doubleParameter, parameters.getDouble("double"));
	}

	@Test
	void testFindJobExecutionsInOrder() {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo").toJobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution1 = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);
		JobExecution jobExecution2 = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);

		// when
		List<JobExecution> jobExecutions = jdbcJobExecutionDao.findJobExecutions(jobInstance);

		// then
		Assertions.assertEquals(2, jobExecutions.size());
		Assertions.assertEquals(jobExecution2.getId(), jobExecutions.get(0).getId());
		Assertions.assertEquals(jobExecution1.getId(), jobExecutions.get(1).getId());
	}

	@Test
	void testGetJobParametersReconstructsEmptyListWhenPersistedValueIsNull() {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addJobParameter("providers", List.of(), List.class)
			.toJobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);
		setPersistedParameterValueToNull(jobExecution.getId(), "providers");

		// when
		JobParameters retrieved = jdbcJobExecutionDao.getJobParameters(jobExecution.getId());

		// then
		JobParameter<?> providers = retrieved.getParameter("providers");
		Assertions.assertNotNull(providers);
		Assertions.assertEquals("providers", providers.name());
		Assertions.assertEquals(List.class, providers.type());
		Assertions.assertTrue(providers.identifying());
		Assertions.assertNotNull(providers.value());
		Assertions.assertEquals(List.of(), providers.value());
	}

	@Test
	void testGetJobParametersReconstructsEmptyStringWhenPersistedValueIsNull() {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("identifyingEmpty", "", true)
			.addString("nonIdentifyingEmpty", "", false)
			.toJobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);
		setPersistedParameterValueToNull(jobExecution.getId(), "identifyingEmpty");
		setPersistedParameterValueToNull(jobExecution.getId(), "nonIdentifyingEmpty");

		// when
		JobParameters retrieved = jdbcJobExecutionDao.getJobParameters(jobExecution.getId());

		// then
		JobParameter<?> identifyingEmpty = retrieved.getParameter("identifyingEmpty");
		Assertions.assertNotNull(identifyingEmpty);
		Assertions.assertEquals("identifyingEmpty", identifyingEmpty.name());
		Assertions.assertEquals(String.class, identifyingEmpty.type());
		Assertions.assertEquals("", identifyingEmpty.value());
		Assertions.assertTrue(identifyingEmpty.identifying());

		JobParameter<?> nonIdentifyingEmpty = retrieved.getParameter("nonIdentifyingEmpty");
		Assertions.assertNotNull(nonIdentifyingEmpty);
		Assertions.assertEquals("nonIdentifyingEmpty", nonIdentifyingEmpty.name());
		Assertions.assertEquals(String.class, nonIdentifyingEmpty.type());
		Assertions.assertEquals("", nonIdentifyingEmpty.value());
		Assertions.assertFalse(nonIdentifyingEmpty.identifying());
	}

	@Test
	void testLastJobExecutionLookupSucceedsAfterReadingNullEmptyParameters() {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addJobParameter("providers", List.of(), List.class)
			.addString("emptyString", "")
			.toJobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution firstExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);
		setPersistedParameterValueToNull(firstExecution.getId(), "providers");
		setPersistedParameterValueToNull(firstExecution.getId(), "emptyString");

		// when
		JobExecution retrievedById = jdbcJobExecutionDao.getJobExecution(firstExecution.getId());
		JobExecution lastBeforeSecond = jdbcJobExecutionDao.getLastJobExecution(jobInstance);
		JobExecution secondExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);
		JobExecution lastAfterSecond = jdbcJobExecutionDao.getLastJobExecution(jobInstance);

		// then
		Assertions.assertNotNull(retrievedById);
		Assertions.assertEquals(firstExecution.getId(), retrievedById.getId());
		Assertions.assertNotNull(lastBeforeSecond);
		Assertions.assertEquals(firstExecution.getId(), lastBeforeSecond.getId());
		Assertions.assertNotNull(lastAfterSecond);
		Assertions.assertEquals(secondExecution.getId(), lastAfterSecond.getId());
	}

	@Test
	void testEmptyAndPopulatedListAndStringRoundTripPreservesWhitespace() {
		// given
		List<String> populatedList = List.of("a", "b");
		JobParameters jobParameters = new JobParametersBuilder().addJobParameter("emptyList", List.of(), List.class)
			.addJobParameter("populatedList", populatedList, List.class)
			.addString("emptyString", "")
			.addString("populatedString", "foo")
			.addString("singleSpace", " ")
			.toJobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);

		// when
		JobParameters retrieved = jdbcJobExecutionDao.getJobParameters(jobExecution.getId());

		// then
		JobParameter<?> emptyList = retrieved.getParameter("emptyList");
		Assertions.assertNotNull(emptyList);
		Assertions.assertEquals("emptyList", emptyList.name());
		Assertions.assertEquals(List.class, emptyList.type());
		Assertions.assertEquals(List.of(), emptyList.value());
		Assertions.assertTrue(emptyList.identifying());

		JobParameter<?> populated = retrieved.getParameter("populatedList");
		Assertions.assertNotNull(populated);
		Assertions.assertEquals("populatedList", populated.name());
		Assertions.assertEquals(List.class, populated.type());
		Assertions.assertEquals(populatedList, populated.value());
		Assertions.assertTrue(populated.identifying());

		Assertions.assertEquals("", retrieved.getString("emptyString"));
		Assertions.assertEquals("foo", retrieved.getString("populatedString"));
		Assertions.assertEquals(" ", retrieved.getString("singleSpace"));
	}

	@Test
	void testNullNumericParameterValueFailsConversion() {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addLong("count", 1L).toJobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);
		setPersistedParameterValueToNull(jobExecution.getId(), "count");

		// when / then
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> jdbcJobExecutionDao.getJobParameters(jobExecution.getId()));
	}

	private void setPersistedParameterValueToNull(long jobExecutionId, String parameterName) {
		jdbcTemplate.update(
				"UPDATE BATCH_JOB_EXECUTION_PARAMS SET PARAMETER_VALUE = NULL WHERE JOB_EXECUTION_ID = ? AND PARAMETER_NAME = ?",
				jobExecutionId, parameterName);
	}

}
