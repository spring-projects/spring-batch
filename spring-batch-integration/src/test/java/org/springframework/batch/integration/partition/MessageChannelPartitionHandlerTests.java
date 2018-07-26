package org.springframework.batch.integration.partition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author Will Schipp
 * @author Michael Minella
 *
 */
@SuppressWarnings("raw")
public class MessageChannelPartitionHandlerTests {

	private MessageChannelPartitionHandler messageChannelPartitionHandler;

	@Test
	public void testNoPartitions() throws Exception {
		//execute with no default set
		messageChannelPartitionHandler = new MessageChannelPartitionHandler();
		//mock
		StepExecution masterStepExecution = mock(StepExecution.class);
		StepExecutionSplitter stepExecutionSplitter = mock(StepExecutionSplitter.class);

		//execute
		Collection<StepExecution> executions = messageChannelPartitionHandler.handle(stepExecutionSplitter, masterStepExecution);
		//verify
		assertTrue(executions.isEmpty());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Test
	public void testHandleNoReply() throws Exception {
		//execute with no default set
		messageChannelPartitionHandler = new MessageChannelPartitionHandler();
		//mock
		StepExecution masterStepExecution = mock(StepExecution.class);
		StepExecutionSplitter stepExecutionSplitter = mock(StepExecutionSplitter.class);
		MessagingTemplate operations = mock(MessagingTemplate.class);
		Message message = mock(Message.class);
		//when
		HashSet<StepExecution> stepExecutions = new HashSet<>();
		stepExecutions.add(new StepExecution("step1", new JobExecution(5L)));
		when(stepExecutionSplitter.split(any(StepExecution.class), eq(1))).thenReturn(stepExecutions);
		when(message.getPayload()).thenReturn(Collections.emptyList());
		when(operations.receive((PollableChannel) any())).thenReturn(message);
		//set
		messageChannelPartitionHandler.setMessagingOperations(operations);

		//execute
		Collection<StepExecution> executions = messageChannelPartitionHandler.handle(stepExecutionSplitter, masterStepExecution);
		//verify
		assertNotNull(executions);
		assertTrue(executions.isEmpty());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Test
	public void testHandleWithReplyChannel() throws Exception {
		//execute with no default set
		messageChannelPartitionHandler = new MessageChannelPartitionHandler();
		//mock
		StepExecution masterStepExecution = mock(StepExecution.class);
		StepExecutionSplitter stepExecutionSplitter = mock(StepExecutionSplitter.class);
		MessagingTemplate operations = mock(MessagingTemplate.class);
		Message message = mock(Message.class);
		PollableChannel replyChannel = mock(PollableChannel.class);
		//when
		HashSet<StepExecution> stepExecutions = new HashSet<>();
		stepExecutions.add(new StepExecution("step1", new JobExecution(5L)));
		when(stepExecutionSplitter.split(any(StepExecution.class), eq(1))).thenReturn(stepExecutions);
		when(message.getPayload()).thenReturn(Collections.emptyList());
		when(operations.receive(replyChannel)).thenReturn(message);
		//set
		messageChannelPartitionHandler.setMessagingOperations(operations);
		messageChannelPartitionHandler.setReplyChannel(replyChannel);

		//execute
		Collection<StepExecution> executions = messageChannelPartitionHandler.handle(stepExecutionSplitter, masterStepExecution);
		//verify
		assertNotNull(executions);
		assertTrue(executions.isEmpty());

	}

	@SuppressWarnings("rawtypes")
	@Test(expected = MessageTimeoutException.class)
	public void messageReceiveTimeout() throws Exception {
		//execute with no default set
		messageChannelPartitionHandler = new MessageChannelPartitionHandler();
		//mock
		StepExecution masterStepExecution = mock(StepExecution.class);
		StepExecutionSplitter stepExecutionSplitter = mock(StepExecutionSplitter.class);
		MessagingTemplate operations = mock(MessagingTemplate.class);
		Message message = mock(Message.class);
		//when
		HashSet<StepExecution> stepExecutions = new HashSet<>();
		stepExecutions.add(new StepExecution("step1", new JobExecution(5L)));
		when(stepExecutionSplitter.split(any(StepExecution.class), eq(1))).thenReturn(stepExecutions);
		when(message.getPayload()).thenReturn(Collections.emptyList());
		//set
		messageChannelPartitionHandler.setMessagingOperations(operations);

		//execute
		messageChannelPartitionHandler.handle(stepExecutionSplitter, masterStepExecution);
	}

	@Test
	public void testHandleWithJobRepositoryPolling() throws Exception {
		//execute with no default set
		messageChannelPartitionHandler = new MessageChannelPartitionHandler();
		//mock
		JobExecution jobExecution = new JobExecution(5L, new JobParameters());
		StepExecution masterStepExecution = new StepExecution("step1", jobExecution, 1L);
		StepExecutionSplitter stepExecutionSplitter = mock(StepExecutionSplitter.class);
		MessagingTemplate operations = mock(MessagingTemplate.class);
		JobExplorer jobExplorer = mock(JobExplorer.class);
		//when
		HashSet<StepExecution> stepExecutions = new HashSet<>();
		StepExecution partition1 = new StepExecution("step1:partition1", jobExecution, 2L);
		StepExecution partition2 = new StepExecution("step1:partition2", jobExecution, 3L);
		StepExecution partition3 = new StepExecution("step1:partition3", jobExecution, 4L);
		StepExecution partition4 = new StepExecution("step1:partition3", jobExecution, 4L);
		partition1.setStatus(BatchStatus.COMPLETED);
		partition2.setStatus(BatchStatus.COMPLETED);
		partition3.setStatus(BatchStatus.STARTED);
		partition4.setStatus(BatchStatus.COMPLETED);
		stepExecutions.add(partition1);
		stepExecutions.add(partition2);
		stepExecutions.add(partition3);
		when(stepExecutionSplitter.split(any(StepExecution.class), eq(1))).thenReturn(stepExecutions);
		when(jobExplorer.getStepExecution(eq(5L), any(Long.class))).thenReturn(partition2, partition1, partition3, partition3, partition3, partition3, partition4);

		//set
		messageChannelPartitionHandler.setMessagingOperations(operations);
		messageChannelPartitionHandler.setJobExplorer(jobExplorer);
		messageChannelPartitionHandler.setStepName("step1");
		messageChannelPartitionHandler.setPollInterval(500L);
		messageChannelPartitionHandler.afterPropertiesSet();

		//execute
		Collection<StepExecution> executions = messageChannelPartitionHandler.handle(stepExecutionSplitter, masterStepExecution);
		//verify
		assertNotNull(executions);
		assertEquals(3, executions.size());
		assertTrue(executions.contains(partition1));
		assertTrue(executions.contains(partition2));
		assertTrue(executions.contains(partition4));

		//verify
		verify(operations, times(3)).send(any(Message.class));
	}

	@Test(expected = TimeoutException.class)
	public void testHandleWithJobRepositoryPollingTimeout() throws Exception {
		//execute with no default set
		messageChannelPartitionHandler = new MessageChannelPartitionHandler();
		//mock
		JobExecution jobExecution = new JobExecution(5L, new JobParameters());
		StepExecution masterStepExecution = new StepExecution("step1", jobExecution, 1L);
		StepExecutionSplitter stepExecutionSplitter = mock(StepExecutionSplitter.class);
		MessagingTemplate operations = mock(MessagingTemplate.class);
		JobExplorer jobExplorer = mock(JobExplorer.class);
		//when
		HashSet<StepExecution> stepExecutions = new HashSet<>();
		StepExecution partition1 = new StepExecution("step1:partition1", jobExecution, 2L);
		StepExecution partition2 = new StepExecution("step1:partition2", jobExecution, 3L);
		StepExecution partition3 = new StepExecution("step1:partition3", jobExecution, 4L);
		partition1.setStatus(BatchStatus.COMPLETED);
		partition2.setStatus(BatchStatus.COMPLETED);
		partition3.setStatus(BatchStatus.STARTED);
		stepExecutions.add(partition1);
		stepExecutions.add(partition2);
		stepExecutions.add(partition3);
		when(stepExecutionSplitter.split(any(StepExecution.class), eq(1))).thenReturn(stepExecutions);
		when(jobExplorer.getStepExecution(eq(5L), any(Long.class))).thenReturn(partition2, partition1, partition3);

		//set
		messageChannelPartitionHandler.setMessagingOperations(operations);
		messageChannelPartitionHandler.setJobExplorer(jobExplorer);
		messageChannelPartitionHandler.setStepName("step1");
		messageChannelPartitionHandler.setTimeout(1000L);
		messageChannelPartitionHandler.afterPropertiesSet();

		//execute
		messageChannelPartitionHandler.handle(stepExecutionSplitter, masterStepExecution);
	}
}
