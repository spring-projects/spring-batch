package org.springframework.batch.integration.partition;

import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
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
		assertNull(executions);
	}

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
		HashSet<StepExecution> stepExecutions = new HashSet<StepExecution>();
		stepExecutions.add(new StepExecution("step1", new JobExecution(5l)));
		when(stepExecutionSplitter.split((StepExecution) anyObject(), eq(1))).thenReturn(stepExecutions);
		when(message.getPayload()).thenReturn(Collections.emptyList());
		when(operations.receive((PollableChannel) anyObject())).thenReturn(message);
		//set
		messageChannelPartitionHandler.setMessagingOperations(operations);

		//execute
		Collection<StepExecution> executions = messageChannelPartitionHandler.handle(stepExecutionSplitter, masterStepExecution);
		//verify
		assertNotNull(executions);
		assertTrue(executions.isEmpty());
	}

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
		HashSet<StepExecution> stepExecutions = new HashSet<StepExecution>();
		stepExecutions.add(new StepExecution("step1", new JobExecution(5l)));
		when(stepExecutionSplitter.split((StepExecution) anyObject(), eq(1))).thenReturn(stepExecutions);
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
		HashSet<StepExecution> stepExecutions = new HashSet<StepExecution>();
		stepExecutions.add(new StepExecution("step1", new JobExecution(5l)));
		when(stepExecutionSplitter.split((StepExecution) anyObject(), eq(1))).thenReturn(stepExecutions);
		when(message.getPayload()).thenReturn(Collections.emptyList());
		//set
		messageChannelPartitionHandler.setMessagingOperations(operations);

		//execute
		Collection<StepExecution> executions = messageChannelPartitionHandler.handle(stepExecutionSplitter, masterStepExecution);
	}
}
