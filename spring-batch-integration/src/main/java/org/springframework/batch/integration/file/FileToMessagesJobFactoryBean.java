/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.integration.file;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.resource.StepExecutionResourceProxy;
import org.springframework.batch.core.step.item.SimpleStepFactoryBean;
import org.springframework.batch.integration.item.MessageChannelItemWriter;
import org.springframework.batch.integration.launch.JobLaunchingMessageHandler;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.dispatcher.DirectChannel;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * A FactoryBean for a {@link Job} with a single step which just pumps messages
 * from a file into a channel. The channel has to be a
 * {@link DirectChannel} to ensure that failures propagate up to the step
 * and fail the job execution. Normally this job will be used in conjunction
 * with a {@link JobLaunchingMessageHandler} and a
 * {@link ResourcePayloadAsJobParameterStrategy}, so that the user can just
 * send a message to a request channel listing the files to be processed, and
 * everything else just happens by magic. After a failure the job will be
 * restarted just by sending it the same message.
 * 
 * @author Dave Syer
 * 
 */
public class FileToMessagesJobFactoryBean implements FactoryBean, BeanNameAware {

	private String name = "fileToMessageJob";

	private ItemReader itemReader;

	private MessageChannel channel;

	private PlatformTransactionManager transactionManager;

	private JobRepository jobRepository;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		this.name = name;
	}

	/**
	 * Public setter for the {@link ItemReader}. Must be either a
	 * {@link FlatFileItemReader} or a {@link StaxEventItemReader}. In either
	 * case there is no need to set the resource property as it will be set by
	 * this factory.
	 * 
	 * @param itemReader the itemReader to set
	 */
	@Required
	public void setItemReader(ItemReader itemReader) {
		this.itemReader = itemReader;
	}

	/**
	 * Public setter for the channel. Each item from the item reader will be
	 * sent to this channel.
	 * @param channel the channel to set
	 */
	@Required
	public void setChannel(MessageChannel channel) {
		this.channel = channel;
	}

	/**
	 * Public setter for the {@link JobRepository}.
	 * @param jobRepository the job repository to set
	 */
	@Required
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Public setter for the {@link PlatformTransactionManager}.
	 * @param transactionManager the transaction manager to set
	 */
	@Required
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Creates a {@link Job} that can process a flat file or XML file into
	 * messages. To launch the job will require only a {@link JobParameters}
	 * instance with a resource location as a URL.
	 * 
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	public Object getObject() throws Exception {

		SimpleJob job = new SimpleJob();
		job.setName(name);
		job.setJobRepository(jobRepository);

		SimpleStepFactoryBean stepFactory = new SimpleStepFactoryBean();
		stepFactory.setBeanName("step");

		Assert.state((itemReader instanceof FlatFileItemReader) || (itemReader instanceof StaxEventItemReader),
				"ItemReader must be either a FlatFileItemReader or a StaxEventItemReader");
		StepExecutionResourceProxy resourceProxy = new StepExecutionResourceProxy();
		resourceProxy.setFilePattern("%" + ResourcePayloadAsJobParameterStrategy.FILE_INPUT_PATH + "%");
		stepFactory.setListeners(new StepExecutionListener[] { resourceProxy });
		setResource(itemReader, resourceProxy);
		stepFactory.setItemReader(itemReader);

		Assert.notNull(channel, "A channel must be provided");
		Assert.state(channel instanceof DirectChannel,
				"The channel must be a DirectChannel (otherwise failures can not be recovered from)");
		MessageChannelItemWriter itemWriter = new MessageChannelItemWriter();
		itemWriter.setChannel(channel);
		stepFactory.setItemWriter(itemWriter);

		Assert.notNull(transactionManager, "A transaction manager must be provided");
		stepFactory.setTransactionManager(transactionManager);

		Assert.notNull(jobRepository, "A job repository must be provided");
		stepFactory.setJobRepository(jobRepository);

		job.addStep((Step) stepFactory.getObject());
		return job;
	}

	/**
	 * @param itemReader
	 * @param resource
	 */
	private void setResource(ItemReader itemReader, Resource resource) {
		if (itemReader instanceof FlatFileItemReader) {
			((FlatFileItemReader) itemReader).setResource(resource);
		}
		else {
			((StaxEventItemReader) itemReader).setResource(resource);
		}
	}

	/**
	 * Always returns {@link Job}.
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	public Class<Job> getObjectType() {
		return Job.class;
	}

	/**
	 * Always true. TODO: should it be false?
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	public boolean isSingleton() {
		return true;
	}

}
