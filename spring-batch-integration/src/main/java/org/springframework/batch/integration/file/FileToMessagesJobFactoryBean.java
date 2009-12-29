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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.item.SimpleStepFactoryBean;
import org.springframework.batch.integration.launch.JobLaunchingMessageHandler;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * A FactoryBean for a {@link Job} with a single step which just pumps messages
 * from a file into a channel. The channel has to be a {@link DirectChannel} to
 * ensure that failures propagate up to the step and fail the job execution.
 * Normally this job will be used in conjunction with a
 * {@link JobLaunchingMessageHandler} and a
 * {@link ResourcePayloadAsJobParameterStrategy}, so that the user can just send
 * a message to a request channel listing the files to be processed, and
 * everything else just happens by magic. After a failure the job will be
 * restarted just by sending it the same message.
 * 
 * @author Dave Syer
 * 
 */
public class FileToMessagesJobFactoryBean<T> implements FactoryBean, BeanNameAware {

	private String name = "fileToMessageJob";

	private ItemReader<? extends T> itemReader;

	private MessageChannel channel;

	private PlatformTransactionManager transactionManager;

	private JobRepository jobRepository;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang
	 * .String)
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
	public void setItemReader(ItemReader<? extends T> itemReader) {
		this.itemReader = itemReader;
	}

	/**
	 * Public setter for the channel. Each item from the item reader will be
	 * sent to this channel.
	 * 
	 * @param channel the channel to set
	 */
	@Required
	public void setChannel(MessageChannel channel) {
		this.channel = channel;
	}

	/**
	 * Public setter for the {@link JobRepository}.
	 * 
	 * @param jobRepository the job repository to set
	 */
	@Required
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Public setter for the {@link PlatformTransactionManager}.
	 * 
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

		SimpleStepFactoryBean<T, T> stepFactory = new SimpleStepFactoryBean<T, T>();
		stepFactory.setBeanName("step");

		Assert.state((itemReader instanceof FlatFileItemReader) || (itemReader instanceof StaxEventItemReader),
				"ItemReader must be either a FlatFileItemReader or a StaxEventItemReader");
		JobParameterResourceProxy resourceProxy = new JobParameterResourceProxy();
		resourceProxy.setKey(ResourcePayloadAsJobParameterStrategy.FILE_INPUT_PATH);
		stepFactory.setListeners(new StepExecutionListener[] { resourceProxy });
		setResource(itemReader, resourceProxy);
		stepFactory.setItemReader(itemReader);

		Assert.notNull(channel, "A channel must be provided");
		Assert.state(channel instanceof DirectChannel,
				"The channel must be a DirectChannel (otherwise failures can not be recovered from)");
		MessageChannelItemWriter<? super T> itemWriter = new MessageChannelItemWriter<T>(channel);
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
	private void setResource(ItemReader<? extends T> itemReader, Resource resource) {
		if (itemReader instanceof FlatFileItemReader) {
			((FlatFileItemReader<? extends T>) itemReader).setResource(resource);
		}
		else {
			((StaxEventItemReader<? extends T>) itemReader).setResource(resource);
		}
	}

	/**
	 * Always returns {@link Job}.
	 * 
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	public Class<Job> getObjectType() {
		return Job.class;
	}

	/**
	 * Always true. TODO: should it be false?
	 * 
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	public boolean isSingleton() {
		return true;
	}

	/**
	 * Strategy for resolving a filename just prior to step execution. The proxy
	 * is given a key that will correspond to a key in the job parameters. Just
	 * before the step is executed, the resource will be created with its
	 * filename as the value found in the job parameters.
	 * 
	 * To use this resource it must be initialised with a {@link StepExecution}.
	 * The best way to do that is to register it as a listener in the step that
	 * is going to need it. For this reason the resource implements
	 * {@link StepExecutionListener}.
	 * 
	 * @see Resource
	 */
	private class JobParameterResourceProxy extends StepExecutionListenerSupport implements Resource,
			ResourceLoaderAware, StepExecutionListener {

		private JobParametersConverter jobParametersConverter = new DefaultJobParametersConverter();

		private ResourceLoader resourceLoader = new FileSystemResourceLoader();

		private Resource delegate;

		private String key = null;

		private static final String NOT_INITIALISED = "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.";

		/**
		 * @param relativePath
		 * @throws IOException
		 * @see org.springframework.core.io.Resource#createRelative(java.lang.String)
		 */
		public Resource createRelative(String relativePath) throws IOException {
			Assert.state(delegate != null, NOT_INITIALISED);
			return delegate.createRelative(relativePath);
		}

		/**
		 * @see org.springframework.core.io.Resource#exists()
		 */
		public boolean exists() {
			Assert.state(delegate != null, NOT_INITIALISED);
			return delegate.exists();
		}

		/**
		 * @see org.springframework.core.io.Resource#getDescription()
		 */
		public String getDescription() {
			Assert.state(delegate != null, NOT_INITIALISED);
			return delegate.getDescription();
		}

		/**
		 * @throws IOException
		 * @see org.springframework.core.io.Resource#getFile()
		 */
		public File getFile() throws IOException {
			Assert.state(delegate != null, NOT_INITIALISED);
			return delegate.getFile();
		}

		/**
		 * @see org.springframework.core.io.Resource#getFilename()
		 */
		public String getFilename() {
			Assert.state(delegate != null, NOT_INITIALISED);
			return delegate.getFilename();
		}

		/**
		 * @throws IOException
		 * @see org.springframework.core.io.InputStreamSource#getInputStream()
		 */
		public InputStream getInputStream() throws IOException {
			Assert.state(delegate != null, NOT_INITIALISED);
			return delegate.getInputStream();
		}

		/**
		 * @throws IOException
		 * @see org.springframework.core.io.Resource#getURI()
		 */
		public URI getURI() throws IOException {
			Assert.state(delegate != null, NOT_INITIALISED);
			return delegate.getURI();
		}

		/**
		 * @throws IOException
		 * @see org.springframework.core.io.Resource#getURL()
		 */
		public URL getURL() throws IOException {
			Assert.state(delegate != null, NOT_INITIALISED);
			return delegate.getURL();
		}

		/**
		 * @see org.springframework.core.io.Resource#isOpen()
		 */
		public boolean isOpen() {
			Assert.state(delegate != null, NOT_INITIALISED);
			return delegate.isOpen();
		}

		/**
		 * @see org.springframework.core.io.Resource#isReadable()
		 */
		public boolean isReadable() {
			Assert.state(delegate != null, NOT_INITIALISED);
			return delegate.isReadable();
		}

		/**
		 * @see org.springframework.core.io.Resource#lastModified()
		 */
		public long lastModified() throws IOException {
			Assert.state(delegate != null, NOT_INITIALISED);
			return delegate.lastModified();
		}

		/**
		 * Public setter for the {@link JobParametersConverter} used to
		 * translate {@link JobParameters} into {@link Properties}. Defaults to
		 * a {@link DefaultJobParametersConverter}.
		 * 
		 * @param jobParametersConverter the {@link JobParametersConverter} to
		 * set
		 */
		public void setJobParametersFactory(JobParametersConverter jobParametersConverter) {
			this.jobParametersConverter = jobParametersConverter;
		}

		/**
		 * Always false because we are expecting to be step scoped.
		 * 
		 * @see org.springframework.beans.factory.config.AbstractFactoryBean#isSingleton()
		 */
		public boolean isSingleton() {
			return false;
		}

		/**
		 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader(org.springframework.core.io.ResourceLoader)
		 */
		public void setResourceLoader(ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
		}

		public void setKey(String key) {
			this.key = key;
		}

		/**
		 * Collect the properties of the enclosing {@link StepExecution} that
		 * will be needed to create a file name.
		 * 
		 * @see org.springframework.batch.core.StepExecutionListener#beforeStep(org.springframework.batch.core.StepExecution)
		 */
		public void beforeStep(StepExecution execution) {
			Properties properties = jobParametersConverter.getProperties(execution.getJobExecution().getJobInstance()
					.getJobParameters());
			delegate = resourceLoader.getResource(properties.getProperty(this.key));
		}
	}

	private static class MessageChannelItemWriter<T> implements ItemWriter<T> {

		private MessageChannel channel;

		public MessageChannelItemWriter(MessageChannel channel) {
			super();
			this.channel = channel;
		}

		public void write(List<? extends T> items) throws Exception {
			for (T item : items) {
				channel.send(new GenericMessage<T>(item));
			}
		}

	}
}
