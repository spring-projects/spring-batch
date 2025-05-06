/*
 * Copyright 2021-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.integration.chunk;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.step.factory.SimpleStepFactoryBean;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig
class ChunkMessageItemWriterIntegrationTests {

	private final ChunkMessageChannelItemWriter<Object> writer = new ChunkMessageChannelItemWriter<>();

	@Autowired
	@Qualifier("requests")
	private MessageChannel requests;

	@Autowired
	@Qualifier("replies")
	private PollableChannel replies;

	private final SimpleStepFactoryBean<Object, Object> factory = new SimpleStepFactoryBean<>();

	private JobRepository jobRepository;

	private static long jobCounter;

	@BeforeEach
	void setUp() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder().generateUniqueName(true)
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.build();
		JdbcTransactionManager transactionManager = new JdbcTransactionManager(embeddedDatabase);
		JdbcJobRepositoryFactoryBean repositoryFactoryBean = new JdbcJobRepositoryFactoryBean();
		repositoryFactoryBean.setDataSource(embeddedDatabase);
		repositoryFactoryBean.setTransactionManager(transactionManager);
		repositoryFactoryBean.afterPropertiesSet();
		jobRepository = repositoryFactoryBean.getObject();
		factory.setJobRepository(jobRepository);
		factory.setTransactionManager(transactionManager);
		factory.setBeanName("step");
		factory.setItemWriter(writer);
		factory.setCommitInterval(4);

		MessagingTemplate gateway = new MessagingTemplate();
		writer.setMessagingOperations(gateway);

		gateway.setDefaultChannel(requests);
		writer.setReplyChannel(replies);
		gateway.setReceiveTimeout(100);

		TestItemWriter.count = 0;

		// Drain queues
		Message<?> message = replies.receive(10);
		while (message != null) {
			message = replies.receive(10);
		}

	}

	@AfterEach
	void tearDown() {
		while (replies.receive(10L) != null) {
		}
	}

	@Test
	void testOpenWithNoState() {
		writer.open(new ExecutionContext());
	}

	@Test
	void testUpdateAndOpenWithState() {
		ExecutionContext executionContext = new ExecutionContext();
		writer.update(executionContext);
		writer.open(executionContext);
		assertEquals(0, executionContext.getInt(ChunkMessageChannelItemWriter.EXPECTED));
		assertEquals(0, executionContext.getInt(ChunkMessageChannelItemWriter.ACTUAL));
	}

	@Test
	void testVanillaIteration() throws Exception {

		factory.setItemReader(
				new ListItemReader<>(Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"))));

		Step step = factory.getObject();

		StepExecution stepExecution = getStepExecution(step);
		step.execute(stepExecution);

		waitForResults(6, 10);

		assertEquals(6, TestItemWriter.count);
		assertEquals(6, stepExecution.getReadCount());

	}

	@Test
	void testSimulatedRestart() throws Exception {

		factory.setItemReader(
				new ListItemReader<>(Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"))));

		Step step = factory.getObject();

		StepExecution stepExecution = getStepExecution(step);

		// Set up context with two messages (chunks) in the backlog
		stepExecution.getExecutionContext().putInt(ChunkMessageChannelItemWriter.EXPECTED, 6);
		stepExecution.getExecutionContext().putInt(ChunkMessageChannelItemWriter.ACTUAL, 4);
		// And make the back log real
		requests.send(getSimpleMessage(stepExecution.getJobExecution().getJobId(), "foo"));
		requests.send(getSimpleMessage(stepExecution.getJobExecution().getJobId(), "bar"));
		step.execute(stepExecution);

		waitForResults(8, 10);

		assertEquals(8, TestItemWriter.count);
		assertEquals(6, stepExecution.getReadCount());

	}

	@Test
	void testSimulatedRestartWithBadMessagesFromAnotherJob() throws Exception {

		factory.setItemReader(
				new ListItemReader<>(Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"))));

		Step step = factory.getObject();

		StepExecution stepExecution = getStepExecution(step);

		// Set up context with two messages (chunks) in the backlog
		stepExecution.getExecutionContext().putInt(ChunkMessageChannelItemWriter.EXPECTED, 3);
		stepExecution.getExecutionContext().putInt(ChunkMessageChannelItemWriter.ACTUAL, 2);

		// Speed up the eventual failure
		writer.setMaxWaitTimeouts(2);

		// And make the back log real
		requests.send(getSimpleMessage(4321L, "foo"));
		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution.getExitStatus().getExitCode());
		String message = stepExecution.getExitStatus().getExitDescription();
		assertTrue(message.contains("wrong job"), "Message does not contain 'wrong job': " + message);

		waitForResults(1, 10);

		assertEquals(1, TestItemWriter.count);
		assertEquals(0, stepExecution.getReadCount());

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private GenericMessage<ChunkRequest> getSimpleMessage(Long jobId, String... items) {
		StepContribution stepContribution = new JobExecution(new JobInstance(0L, "job"), new JobParameters())
			.createStepExecution("step")
			.createStepContribution();
		ChunkRequest chunk = new ChunkRequest(0, Chunk.of(items), jobId, stepContribution);
		GenericMessage<ChunkRequest> message = new GenericMessage<>(chunk);
		return message;
	}

	@Test
	void testEarlyCompletionSignalledInHandler() throws Exception {

		factory.setItemReader(
				new ListItemReader<>(Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,fail,3,4,5,6"))));
		factory.setCommitInterval(2);

		Step step = factory.getObject();

		StepExecution stepExecution = getStepExecution(step);
		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution.getExitStatus().getExitCode());
		String message = stepExecution.getExitStatus().getExitDescription();
		assertTrue(message.contains("fail"), "Message does not contain 'fail': " + message);

		waitForResults(2, 10);

		// The number of items processed is actually between 1 and 6, because
		// the one that failed might have been processed out of order.
		assertTrue(1 <= TestItemWriter.count);
		assertTrue(6 >= TestItemWriter.count);
		// But it should fail the step in any case
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());

	}

	@Test
	void testSimulatedRestartWithNoBacklog() throws Exception {

		factory.setItemReader(
				new ListItemReader<>(Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"))));

		Step step = factory.getObject();

		StepExecution stepExecution = getStepExecution(step);

		// Set up expectation of three messages (chunks) in the backlog
		stepExecution.getExecutionContext().putInt(ChunkMessageChannelItemWriter.EXPECTED, 6);
		stepExecution.getExecutionContext().putInt(ChunkMessageChannelItemWriter.ACTUAL, 3);

		writer.setMaxWaitTimeouts(2);

		/*
		 * With no backlog we process all the items, but the listener can't reconcile the
		 * expected number of items with the actual. An infinite loop would be bad, so the
		 * best we can do is fail as fast as possible.
		 */
		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution.getExitStatus().getExitCode());
		String message = stepExecution.getExitStatus().getExitDescription();
		assertTrue(message.toLowerCase().contains("timed out"), "Message did not contain 'timed out': " + message);

		assertEquals(0, TestItemWriter.count);
		assertEquals(0, stepExecution.getReadCount());

	}

	/**
	 * This one is flakey - we try to force it to wait until after the step to finish
	 * processing just by waiting for long enough.
	 */
	@Test
	void testFailureInStepListener() throws Exception {

		factory.setItemReader(
				new ListItemReader<>(Arrays.asList(StringUtils.commaDelimitedListToStringArray("wait,fail,3,4,5,6"))));

		Step step = factory.getObject();

		StepExecution stepExecution = getStepExecution(step);
		step.execute(stepExecution);

		waitForResults(2, 10);

		// The number of items processed is actually between 1 and 6, because
		// the one that failed might have been processed out of order.
		assertTrue(1 <= TestItemWriter.count);
		assertTrue(6 >= TestItemWriter.count);

		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution.getExitStatus().getExitCode());

		String exitDescription = stepExecution.getExitStatus().getExitDescription();
		assertTrue(exitDescription.contains(AsynchronousFailureException.class.getName()),
				"Exit description does not contain exception type name: " + exitDescription);

	}

	// TODO : test non-dispatch of empty chunk

	private void waitForResults(int expected, int maxWait) throws InterruptedException {
		int count = 0;
		while (TestItemWriter.count < expected && count < maxWait) {
			count++;
			Thread.sleep(10);
		}
	}

	private StepExecution getStepExecution(Step step)
			throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
		SimpleJob job = new SimpleJob();
		job.setName("job");
		JobExecution jobExecution = jobRepository.createJobExecution(job.getName(),
				new JobParametersBuilder().addLong("job.counter", jobCounter++).toJobParameters());
		StepExecution stepExecution = jobExecution.createStepExecution(step.getName());
		jobRepository.add(stepExecution);
		return stepExecution;
	}

}
