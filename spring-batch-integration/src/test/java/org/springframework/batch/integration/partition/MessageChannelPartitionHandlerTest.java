package org.springframework.batch.integration.partition;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessagingOperations;
import org.springframework.integration.core.PollableChannel;

/**
 * 
 * @author Will Schipp
 *
 */
public class MessageChannelPartitionHandlerTest {

	private MessageChannelPartitionHandler messageChannelPartitionHandler;
	
	@Test
	public void testHandleNoReply() throws Exception {
		//execute with no default set
		messageChannelPartitionHandler = new MessageChannelPartitionHandler();
		//mock
		StepExecution masterStepExecution = mock(StepExecution.class);
		StepExecutionSplitter stepExecutionSplitter = mock(StepExecutionSplitter.class);
		MessagingOperations operations = mock(MessagingOperations.class);
		Message message = mock(Message.class);
		//when
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
		MessagingOperations operations = mock(MessagingOperations.class);
		Message message = mock(Message.class);
		PollableChannel replyChannel = mock(PollableChannel.class);
		//when
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

}
