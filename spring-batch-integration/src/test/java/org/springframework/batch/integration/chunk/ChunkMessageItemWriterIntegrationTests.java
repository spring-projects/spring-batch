package org.springframework.batch.integration.chunk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.repository.dao.MapExecutionContextDao;
import org.springframework.batch.core.repository.dao.MapJobExecutionDao;
import org.springframework.batch.core.repository.dao.MapJobInstanceDao;
import org.springframework.batch.core.repository.dao.MapStepExecutionDao;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.springframework.batch.core.step.item.SimpleStepFactoryBean;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ChunkMessageItemWriterIntegrationTests {

	private ChunkMessageChannelItemWriter<Object> writer = new ChunkMessageChannelItemWriter<Object>();

	@Autowired
	@Qualifier("requests")
	private PollableChannel requests;

	@Autowired
	@Qualifier("replies")
	private PollableChannel replies;

	private SimpleStepFactoryBean<Object> factory;

	private SimpleJobRepository jobRepository;

	private static long jobCounter;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() {

		factory = new SimpleStepFactoryBean<Object>();
		jobRepository = new SimpleJobRepository(new MapJobInstanceDao(),
				new MapJobExecutionDao(), new MapStepExecutionDao(), new MapExecutionContextDao());
		factory.setJobRepository(jobRepository);
		factory.setTransactionManager(new ResourcelessTransactionManager());
		factory.setBeanName("step");
		factory.setItemWriter(writer);
		factory.setCommitInterval(4);

		writer.setSource(replies);
		writer.setTarget(requests);

		TestItemWriter.count = 0;
		
		// Drain queues
		Message<?> message = requests.receive(10);
		while (message!=null) {
			System.err.println(message);
			message = requests.receive(10);
		}
		message = replies.receive(10);
		while (message!=null) {
			System.err.println(message);
			message = replies.receive(10);
		}

	}

	@After
	public void tearDown() {
		while(requests.receive(10L)!=null) {}
		while(replies.receive(10L)!=null) {}
	}

	@Test
	public void testOpenWithNoState() throws Exception {
		writer.open(new ExecutionContext());
	}

	@Test
	public void testUpdateAndOpenWithState() throws Exception {
		ExecutionContext executionContext = new ExecutionContext();
		writer.update(executionContext);
		writer.open(executionContext);
		assertEquals(0, executionContext
				.getLong(ChunkMessageChannelItemWriter.EXPECTED));
		assertEquals(0, executionContext
				.getLong(ChunkMessageChannelItemWriter.ACTUAL));
	}

	@Test
	public void testVanillaIteration() throws Exception {

		factory.setItemReader(new ListItemReader<String>(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("1,2,3,4,5,6"))));

		Step step = (Step) factory.getObject();

		StepExecution stepExecution = getStepExecution(step);
		step.execute(stepExecution);
		
		waitForResults(6, 10);

		assertEquals(6, TestItemWriter.count);
		assertEquals(6, stepExecution.getItemCount().intValue());

	}

	@Test
	public void testSimulatedRestart() throws Exception {

		factory.setItemReader(new ListItemReader<String>(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("1,2,3,4,5,6"))));

		Step step = (Step) factory.getObject();

		StepExecution stepExecution = getStepExecution(step);

		// Set up context with two messages (chunks) in the backlog
		stepExecution.getExecutionContext().putLong(
				ChunkMessageChannelItemWriter.EXPECTED, 6);
		stepExecution.getExecutionContext().putLong(
				ChunkMessageChannelItemWriter.ACTUAL, 4);
		// And make the back log real
		requests.send(getSimpleMessage("foo", stepExecution.getJobExecution().getJobId()));
		requests.send(getSimpleMessage("bar", stepExecution.getJobExecution().getJobId()));
		step.execute(stepExecution);

		waitForResults(8, 10);

		assertEquals(8, TestItemWriter.count);
		assertEquals(6, stepExecution.getItemCount().intValue());

	}

	@Test
	public void testSimulatedRestartWithBadMessagesFromAnotherJob() throws Exception {

		factory.setItemReader(new ListItemReader<String>(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("1,2,3,4,5,6"))));

		Step step = (Step) factory.getObject();

		StepExecution stepExecution = getStepExecution(step);

		// Set up context with two messages (chunks) in the backlog
		stepExecution.getExecutionContext().putLong(
				ChunkMessageChannelItemWriter.EXPECTED, 3);
		stepExecution.getExecutionContext().putLong(
				ChunkMessageChannelItemWriter.ACTUAL, 2);
		// And make the back log real
		requests.send(getSimpleMessage("foo", new Long(4321)));
		try {
			step.execute(stepExecution);
			fail("Expected UnexpectedJobExecutionException");
		} catch (UnexpectedJobExecutionException e) {
			String message = e.getCause().getMessage();
			assertTrue("Message does not contain 'wrong job': "+message, message.contains("wrong job"));
		}

		waitForResults(1, 10);

		assertEquals(1, TestItemWriter.count);
		assertEquals(0, stepExecution.getItemCount().intValue());

	}

	/**
	 * @param jobId 
	 * @param string 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private GenericMessage<ChunkRequest> getSimpleMessage(String string, Long jobId) {
		ChunkRequest chunk = new ChunkRequest(StringUtils
				.commaDelimitedListToSet(string), jobId, 0);
		GenericMessage<ChunkRequest> message = new GenericMessage<ChunkRequest>(chunk);
		return message;
	}

	@Test
	public void testEarlyCompletionSignalledInHandler() throws Exception {

		factory.setItemReader(new ListItemReader<String>(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("1,bad,3,4,5,6"))));
		factory.setCommitInterval(2);

		Step step = (Step) factory.getObject();

		StepExecution stepExecution = getStepExecution(step);
		try {
			step.execute(stepExecution);
			fail("Expected AsynchronousFailureException");
		} catch (AsynchronousFailureException e) {
			assertTrue(e.getMessage().contains("bad"));
		}

		waitForResults(2, 10);

		// The number of items processed is actually between 1 and 6, because
		// the one that failed might have been processed out of order.
		assertTrue(1 <= TestItemWriter.count);
		assertTrue(6 >= TestItemWriter.count);
		// But it should fail the step in any case
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());

	}

	@Test
	public void testSimulatedRestartWithNoBacklog() throws Exception {

		factory.setItemReader(new ListItemReader<String>(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("1,2,3,4,5,6"))));

		Step step = (Step) factory.getObject();

		StepExecution stepExecution = getStepExecution(step);

		// Set up expectation of three messages (chunks) in the backlog
		stepExecution.getExecutionContext().putLong(
				ChunkMessageChannelItemWriter.EXPECTED, 6);
		stepExecution.getExecutionContext().putLong(
				ChunkMessageChannelItemWriter.ACTUAL, 3);
		/*
		 * With no backlog we process all the items, but the listener can't
		 * reconcile the expected number of items with the actual. An infinite
		 * loop would be bad, so the best we can do is fail as fast as possible.
		 */
		try {
			step.execute(stepExecution);
			fail("Expected UnexpectedJobExecutionException");
		} catch (UnexpectedJobExecutionException e) {
			String message = e.getCause().getMessage();
			assertTrue("Message did not contain 'timed out': " + message,
					message.toLowerCase().contains("timed out"));
		}

		assertEquals(0, TestItemWriter.count);
		assertEquals(0, stepExecution.getItemCount().intValue());

	}

	/**
	 * This one is flakey - we try to force it to wait until after the step to
	 * finish processing just by waiting for long enough.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFailureInStepListener() throws Exception {

		factory.setItemReader(new ListItemReader<String>(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("wait,bad,3,4,5,6"))));

		Step step = (Step) factory.getObject();

		StepExecution stepExecution = getStepExecution(step);
		step.execute(stepExecution);

		waitForResults(2, 10);

		// The number of items processed is actually between 1 and 6, because
		// the one that failed might have been processed out of order.
		assertTrue(1 <= TestItemWriter.count);
		assertTrue(6 >= TestItemWriter.count);

		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution
				.getExitStatus().getExitCode());

		String exitDescription = stepExecution.getExitStatus()
				.getExitDescription();
		assertTrue("Exit description does not contain exception type name: "
				+ exitDescription, exitDescription
				.contains(AsynchronousFailureException.class.getName()));

	}

	// TODO: test failure in chunk handler
	// TODO : test non-dispatch of empty chunk

	/**
	 * @param expected
	 * @param maxWait
	 * @throws InterruptedException 
	 */
	private void waitForResults(int expected, int maxWait) throws InterruptedException {
		int count = 0;
		while (TestItemWriter.count<expected && count<maxWait) {
			count++;
			Thread.sleep(10);
		}
	}

	private StepExecution getStepExecution(Step step)
			throws JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException {
		SimpleJob job = new SimpleJob();
		job.setName("job");
		JobExecution jobExecution = jobRepository.createJobExecution(job,
				new JobParametersBuilder().addLong("job.counter", jobCounter++)
						.toJobParameters());
		StepExecution stepExecution = jobExecution.createStepExecution(step);
		return stepExecution;
	}

}
