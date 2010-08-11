package org.springframework.batch.integration.partition;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingOperations;
import org.springframework.integration.core.PollableChannel;
import org.springframework.util.Assert;

/**
 * A {@link PartitionHandler} that uses {@link MessageChannel} instances to send
 * instructions to remote workers and receive their responses. The
 * {@link MessageChannel} provides a nice abstraction so that the location of
 * the workers and the transport used to communicate with them can be changed at
 * run time. The communication with the remote workers does not need to be
 * transactional or have guaranteed delivery, so a local thread pool based
 * implementation works as well as a remote web service or JMS implementation.
 * If a remote worker fails or doesn't send a reply message, the job will fail
 * and can be restarted to pick up missing messages and processing. The remote
 * workers need access to the Spring Batch {@link JobRepository} so that the
 * shared state across those restarts can be managed centrally.
 * 
 * @author Dave Syer
 * 
 */
@MessageEndpoint
public class MessageChannelPartitionHandler implements PartitionHandler {

	private int gridSize = 1;

	private MessagingOperations messagingGateway;

	private String stepName;

	private PollableChannel replyChannel;

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(stepName, "A step name must be provided for the remote workers.");
		Assert.state(messagingGateway != null, "The MessagingOperations must be set");
	}

	/**
	 * A pre-configured gateway for sending and receiving messages to the remote
	 * workers. Using this property allows a large degree of control over the
	 * timeouts and other properties of the send. It should have channels set up
	 * internally:
	 * <ul>
	 * <li>request channel capable of accepting {@link StepExecutionRequest}
	 * payloads</li>
	 * <li>reply channel that returns a list of {@link StepExecution} results</li>
	 * </ul>
	 * The timeout for the repoy should be set sufficiently long that the remote
	 * steps have time to complete.
	 * 
	 * @param messagingGateway the {@link MessagingOperations} to set
	 */
	public void setMessagingOperations(MessagingOperations messagingGateway) {
		this.messagingGateway = messagingGateway;
	}

	public void setReplyChannel(PollableChannel replyChannel) {
		this.replyChannel = replyChannel;
	}

	/**
	 * Passed to the {@link StepExecutionSplitter} in the
	 * {@link #handle(StepExecutionSplitter, StepExecution)} method, instructing
	 * it how many {@link StepExecution} instances are required, ideally. The
	 * {@link StepExecutionSplitter} is allowed to ignore the grid size in the
	 * case of a restart, since the input data partitions must be preserved.
	 * 
	 * @param gridSize the number of step executions that will be created
	 */
	public void setGridSize(int gridSize) {
		this.gridSize = gridSize;
	}

	/**
	 * The name of the {@link Step} that will be used to execute the partitioned
	 * {@link StepExecution}. This is a regular Spring Batch step, with all the
	 * business logic required to complete an execution based on the input
	 * parameters in its {@link StepExecution} context. The name will be
	 * translated into a {@link Step} instance by the remote worker.
	 * 
	 * @param stepName the name of the {@link Step} instance to execute business
	 * logic
	 */
	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

	/**
	 * @param messages the messages to be aggregated
	 * @return the list as it was passed in
	 */
	@Aggregator(sendPartialResultsOnExpiry = true)
	public List<?> aggregate(List<?> messages) {
		return messages;
	}

	/**
	 * Sends {@link StepExecutionRequest} objects to the request channel of the
	 * {@link MessagingOperations}, and then receives the result back as a list of
	 * {@link StepExecution} on a reply channel. Use the
	 * {@link #aggregate(List)} method as an aggregator of the individual remote
	 * replies. The receive timeout needs to be set realistically in the
	 * {@link MessagingOperations} <b>and</b> the aggregator, so that there is a
	 * good chance of all work being done.
	 * 
	 * @see PartitionHandler#handle(StepExecutionSplitter, StepExecution)
	 */
	public Collection<StepExecution> handle(StepExecutionSplitter stepExecutionSplitter,
			StepExecution masterStepExecution) throws Exception {

		Set<StepExecution> split = stepExecutionSplitter.split(masterStepExecution, gridSize);
		int count = 0;
		for (StepExecution stepExecution : split) {
			messagingGateway.send(createMessage(count++, split.size(), new StepExecutionRequest(stepName, stepExecution
					.getJobExecutionId(), stepExecution.getId())));
		}

		Message<Collection<StepExecution>> message = messagingGateway.receive(replyChannel);
		Collection<StepExecution> result = message.getPayload();
		return result;

	}

	private Message<StepExecutionRequest> createMessage(int sequenceNumber, int sequenceSize,
			StepExecutionRequest stepExecutionRequest) {
		return MessageBuilder.withPayload(stepExecutionRequest).setSequenceNumber(sequenceNumber).setSequenceSize(
				sequenceSize).setCorrelationId(
				stepExecutionRequest.getJobExecutionId() + ":" + stepExecutionRequest.getStepName()).build();
	}
}
