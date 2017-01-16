package org.springframework.batch.integration.partition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.poller.DirectPoller;
import org.springframework.batch.poller.Poller;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Payloads;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * A {@link PartitionHandler} that uses {@link MessageChannel} instances to send instructions to remote workers and
 * receive their responses. The {@link MessageChannel} provides a nice abstraction so that the location of the workers
 * and the transport used to communicate with them can be changed at run time. The communication with the remote workers
 * does not need to be transactional or have guaranteed delivery, so a local thread pool based implementation works as
 * well as a remote web service or JMS implementation. If a remote worker fails, the job will fail and can be restarted
 * to pick up missing messages and processing. The remote workers need access to the Spring Batch {@link JobRepository}
 * so that the shared state across those restarts can be managed centrally.
 *
 * While a {@link org.springframework.messaging.MessageChannel} is used for sending the requests to the workers, the
 * worker's responses can be obtained in one of two ways:
 * <ul>
 *     <li>A reply channel - Slaves will respond with messages that will be aggregated via this component.</li>
 *     <li>Polling the job repository - Since the state of each slave is maintained independently within the job
 *     repository, we can poll the store to determine the state without the need of the slaves to formally respond.</li>
 * </ul>
 *
 * Note: The reply channel for this is instance based.  Sharing this component across
 * multiple step instances may result in the crossing of messages.  It's recommended that
 * this component be step or job scoped.
 *
 * @author Dave Syer
 * @author Will Schipp
 * @author Michael Minella
 *
 */
@MessageEndpoint
public class MessageChannelPartitionHandler implements PartitionHandler, InitializingBean {

	private static Log logger = LogFactory.getLog(MessageChannelPartitionHandler.class);

	private int gridSize = 1;

	private MessagingTemplate messagingGateway;

	private String stepName;

	private long pollInterval = 10000;

	private JobExplorer jobExplorer;

	private boolean pollRepositoryForResults = false;

	private long timeout = -1;

	private DataSource dataSource;

	/**
	 * pollable channel for the replies
	 */
	private PollableChannel replyChannel;

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(stepName, "A step name must be provided for the remote workers.");
		Assert.state(messagingGateway != null, "The MessagingOperations must be set");

		pollRepositoryForResults = !(dataSource == null && jobExplorer == null);

		if(pollRepositoryForResults) {
			logger.debug("MessageChannelPartitionHandler is configured to poll the job repository for slave results");
		}

		if(dataSource != null && jobExplorer == null) {
			JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
			jobExplorerFactoryBean.setDataSource(dataSource);
			jobExplorerFactoryBean.afterPropertiesSet();
			jobExplorer = jobExplorerFactoryBean.getObject();
		}

		if (!pollRepositoryForResults && replyChannel == null) {
			replyChannel = new QueueChannel();
		}//end if

	}

	/**
	 * When using job repository polling, the time limit to wait.
	 *
	 * @param timeout millisconds to wait, defaults to -1 (no timeout).
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * {@link org.springframework.batch.core.explore.JobExplorer} to use to query the job repository.  Either this or
	 * a {@link javax.sql.DataSource} is required when using job repository polling.
	 *
	 * @param jobExplorer {@link org.springframework.batch.core.explore.JobExplorer} to use for lookups
	 */
	public void setJobExplorer(JobExplorer jobExplorer) {
		this.jobExplorer = jobExplorer;
	}

	/**
	 * How often to poll the job repository for the status of the slaves.
	 *
	 * @param pollInterval milliseconds between polls, defaults to 10000 (10 seconds).
	 */
	public void setPollInterval(long pollInterval) {
		this.pollInterval = pollInterval;
	}

	/**
	 * {@link javax.sql.DataSource} pointing to the job repository
	 *
	 * @param dataSource {@link javax.sql.DataSource} that points to the job repository's store
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * A pre-configured gateway for sending and receiving messages to the remote workers. Using this property allows a
	 * large degree of control over the timeouts and other properties of the send. It should have channels set up
	 * internally: <ul> <li>request channel capable of accepting {@link StepExecutionRequest} payloads</li> <li>reply
	 * channel that returns a list of {@link StepExecution} results</li> </ul> The timeout for the repoy should be set
	 * sufficiently long that the remote steps have time to complete.
	 *
	 * @param messagingGateway the {@link org.springframework.integration.core.MessagingTemplate} to set
	 */
	public void setMessagingOperations(MessagingTemplate messagingGateway) {
		this.messagingGateway = messagingGateway;
	}

	/**
	 * Passed to the {@link StepExecutionSplitter} in the {@link #handle(StepExecutionSplitter, StepExecution)} method,
	 * instructing it how many {@link StepExecution} instances are required, ideally. The {@link StepExecutionSplitter}
	 * is allowed to ignore the grid size in the case of a restart, since the input data partitions must be preserved.
	 *
	 * @param gridSize the number of step executions that will be created
	 */
	public void setGridSize(int gridSize) {
		this.gridSize = gridSize;
	}

	/**
	 * The name of the {@link Step} that will be used to execute the partitioned {@link StepExecution}. This is a
	 * regular Spring Batch step, with all the business logic required to complete an execution based on the input
	 * parameters in its {@link StepExecution} context. The name will be translated into a {@link Step} instance by the
	 * remote worker.
	 *
	 * @param stepName the name of the {@link Step} instance to execute business logic
	 */
	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

	/**
	 * @param messages the messages to be aggregated
	 * @return the list as it was passed in
	 */
	@Aggregator(sendPartialResultsOnExpiry = "true")
	public List<?> aggregate(@Payloads List<?> messages) {
		return messages;
	}

	public void setReplyChannel(PollableChannel replyChannel) {
		this.replyChannel = replyChannel;
	}

	/**
	 * Sends {@link StepExecutionRequest} objects to the request channel of the {@link MessagingTemplate}, and then
	 * receives the result back as a list of {@link StepExecution} on a reply channel. Use the {@link #aggregate(List)}
	 * method as an aggregator of the individual remote replies. The receive timeout needs to be set realistically in
	 * the {@link MessagingTemplate} <b>and</b> the aggregator, so that there is a good chance of all work being done.
	 *
	 * @see PartitionHandler#handle(StepExecutionSplitter, StepExecution)
	 */
	public Collection<StepExecution> handle(StepExecutionSplitter stepExecutionSplitter,
			final StepExecution masterStepExecution) throws Exception {

		final Set<StepExecution> split = stepExecutionSplitter.split(masterStepExecution, gridSize);

		if(CollectionUtils.isEmpty(split)) {
			return null;
		}

		int count = 0;

		for (StepExecution stepExecution : split) {
			Message<StepExecutionRequest> request = createMessage(count++, split.size(), new StepExecutionRequest(
					stepName, stepExecution.getJobExecutionId(), stepExecution.getId()), replyChannel);
			if (logger.isDebugEnabled()) {
				logger.debug("Sending request: " + request);
			}
			messagingGateway.send(request);
		}

		if(!pollRepositoryForResults) {
			return receiveReplies(replyChannel);
		}
		else {
			return pollReplies(masterStepExecution, split);
		}
	}

	private Collection<StepExecution> pollReplies(final StepExecution masterStepExecution, final Set<StepExecution> split) throws Exception {
		final Collection<StepExecution> result = new ArrayList<StepExecution>(split.size());

		Callable<Collection<StepExecution>> callback = new Callable<Collection<StepExecution>>() {
			@Override
			public Collection<StepExecution> call() throws Exception {

				for(Iterator<StepExecution> stepExecutionIterator = split.iterator(); stepExecutionIterator.hasNext(); ) {
					StepExecution curStepExecution = stepExecutionIterator.next();

					if(!result.contains(curStepExecution)) {
						StepExecution partitionStepExecution =
								jobExplorer.getStepExecution(masterStepExecution.getJobExecutionId(), curStepExecution.getId());

						if(!partitionStepExecution.getStatus().isRunning()) {
							result.add(partitionStepExecution);
						}
					}
				}

				if(logger.isDebugEnabled()) {
					logger.debug(String.format("Currently waiting on %s partitions to finish", split.size()));
				}

				if(result.size() == split.size()) {
					return result;
				}
				else {
					return null;
				}
			}
		};

		Poller<Collection<StepExecution>> poller = new DirectPoller<Collection<StepExecution>>(pollInterval);
		Future<Collection<StepExecution>> resultsFuture = poller.poll(callback);

		if(timeout >= 0) {
			return resultsFuture.get(timeout, TimeUnit.MILLISECONDS);
		}
		else {
			return resultsFuture.get();
		}
	}

	private Collection<StepExecution> receiveReplies(PollableChannel currentReplyChannel) {
		@SuppressWarnings("unchecked")
		Message<Collection<StepExecution>> message = (Message<Collection<StepExecution>>) messagingGateway.receive(currentReplyChannel);

		if(message == null) {
			throw new MessageTimeoutException("Timeout occurred before all partitions returned");
		} else if (logger.isDebugEnabled()) {
			logger.debug("Received replies: " + message);
		}

		return message.getPayload();
	}

	private Message<StepExecutionRequest> createMessage(int sequenceNumber, int sequenceSize,
			StepExecutionRequest stepExecutionRequest, PollableChannel replyChannel) {
		return MessageBuilder.withPayload(stepExecutionRequest).setSequenceNumber(sequenceNumber)
				.setSequenceSize(sequenceSize)
				.setCorrelationId(stepExecutionRequest.getJobExecutionId() + ":" + stepExecutionRequest.getStepName())
				.setReplyChannel(replyChannel)
				.build();
	}
}
