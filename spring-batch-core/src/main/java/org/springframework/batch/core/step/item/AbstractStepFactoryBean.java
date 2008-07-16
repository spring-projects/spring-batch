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
package org.springframework.batch.core.step.item;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.validator.Validator;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.Assert;

/**
 * Base class for factory beans for {@link ItemOrientedStep}. Ensures that all
 * the mandatory properties are set, and provides basic support for the
 * {@link Step} interface responsibilities like start limit. Supports
 * registration of {@link ItemStream}s and {@link StepListener}s.
 * 
 * @see SimpleStepFactoryBean
 * @see RepeatOperationsStepFactoryBean
 * 
 * @author Dave Syer
 * 
 */
public abstract class AbstractStepFactoryBean implements FactoryBean, BeanNameAware {

	private String name;

	private int startLimit = Integer.MAX_VALUE;

	private boolean allowStartIfComplete;

	private ItemReader itemReader;

	private ItemWriter itemWriter;

	private PlatformTransactionManager transactionManager;
	
	private TransactionAttribute transactionAttribute;

	private JobRepository jobRepository;

	private boolean singleton = true;

	private Validator jobRepositoryValidator = new TransactionInterceptorValidator(1);

	private ItemStream[] streams = new ItemStream[0];

	private StepListener[] listeners = new StepListener[0];

	/**
	 * 
	 */
	public AbstractStepFactoryBean() {
		super();
	}

	/**
	 * Set the bean name property, which will become the name of the
	 * {@link Step} when it is created.
	 * 
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		this.name = name;
	}

	/**
	 * Public getter for the String.
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Public setter for the startLimit.
	 * 
	 * @param startLimit the startLimit to set
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
	}

	/**
	 * Public setter for the shouldAllowStartIfComplete.
	 * 
	 * @param allowStartIfComplete the shouldAllowStartIfComplete to set
	 */
	public void setAllowStartIfComplete(boolean allowStartIfComplete) {
		this.allowStartIfComplete = allowStartIfComplete;
	}

	/**
	 * @param itemReader the itemReader to set
	 */
	public void setItemReader(ItemReader itemReader) {
		this.itemReader = itemReader;
	}

	/**
	 * @param itemWriter the itemWriter to set
	 */
	public void setItemWriter(ItemWriter itemWriter) {
		this.itemWriter = itemWriter;
	}

	/**
	 * The streams to inject into the {@link Step}. Any instance of
	 * {@link ItemStream} can be used, and will then receive callbacks at the
	 * appropriate stage in the step.
	 * 
	 * @param streams an array of listeners
	 */
	public void setStreams(ItemStream[] streams) {
		this.streams = streams;
	}

	/**
	 * The listeners to inject into the {@link Step}. Any instance of
	 * {@link StepListener} can be used, and will then receive callbacks at the
	 * appropriate stage in the step.
	 * 
	 * @param listeners an array of listeners
	 */
	public void setListeners(StepListener[] listeners) {
		this.listeners = listeners;
	}

	/**
	 * Protected getter for the {@link StepListener}s.
	 * @return the listeners
	 */
	protected StepListener[] getListeners() {
		return listeners;
	}

	/**
	 * Protected getter for the {@link ItemReader} for subclasses to use.
	 * @return the itemReader
	 */
	protected ItemReader getItemReader() {
		return itemReader;
	}

	/**
	 * Protected getter for the {@link ItemWriter} for subclasses to use
	 * @return the itemWriter
	 */
	protected ItemWriter getItemWriter() {
		return itemWriter;
	}

	/**
	 * Public setter for {@link JobRepository}.
	 * 
	 * @param jobRepository is a mandatory dependence (no default).
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Public setter for the {@link PlatformTransactionManager}.
	 * 
	 * @param transactionManager the transaction manager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Public setter for the {@link TransactionAttribute}.
	 * @param transactionAttribute the {@link TransactionAttribute} to set
	 */
	public void setTransactionAttribute(TransactionAttribute transactionAttribute) {
		this.transactionAttribute = transactionAttribute;
	}

	/**
	 * Protected getter for the {@link TransactionAttribute} for subclasses only.
	 * @return the transactionAttribute
	 */
	protected TransactionAttribute getTransactionAttribute() {
		return transactionAttribute!=null?transactionAttribute:new DefaultTransactionAttribute();
	}

	/**
	 * Create a {@link Step} from the configuration provided.
	 * 
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	public final Object getObject() throws Exception {
		ItemOrientedStep step = new ItemOrientedStep(getName());
		applyConfiguration(step);
		return step;
	}

	/**
	 * @param step
	 * 
	 */
	protected void applyConfiguration(ItemOrientedStep step) {

		Assert.notNull(getItemReader(), "ItemReader must be provided");
		Assert.notNull(getItemWriter(), "ItemWriter must be provided");
		Assert.notNull(transactionManager, "TransactionManager must be provided");
		jobRepositoryValidator.validate(jobRepository);

		step.setItemHandler(new SimpleItemHandler(itemReader, itemWriter));
		step.setTransactionManager(transactionManager);
		if (transactionAttribute!=null) {
			step.setTransactionAttribute(transactionAttribute);
		}
		step.setJobRepository(jobRepository);
		step.setStartLimit(startLimit);
		step.setAllowStartIfComplete(allowStartIfComplete);

		step.setStreams(streams);

		ItemReader itemReader = getItemReader();
		ItemWriter itemWriter = getItemWriter();

		// Since we are going to wrap these things with listener callbacks we
		// need to register them here because the step will not know we did
		// that.
		if (itemReader instanceof ItemStream) {
			step.registerStream((ItemStream) itemReader);
		}
		if (itemReader instanceof StepExecutionListener) {
			step.registerStepExecutionListener((StepExecutionListener) itemReader);
		}
		if (itemWriter instanceof ItemStream) {
			step.registerStream((ItemStream) itemWriter);
		}
		if (itemWriter instanceof StepExecutionListener) {
			step.registerStepExecutionListener((StepExecutionListener) itemWriter);
		}

		StepExecutionListener[] stepListeners = BatchListenerFactoryHelper.getStepListeners(listeners);
		itemReader = BatchListenerFactoryHelper.getItemReader(itemReader, listeners);
		itemWriter = BatchListenerFactoryHelper.getItemWriter(itemWriter, listeners);

		// In case they are used by subclasses:
		setItemReader(itemReader);
		setItemWriter(itemWriter);

		step.setStepExecutionListeners(stepListeners);
		step.setItemHandler(new SimpleItemHandler(itemReader, itemWriter));

	}

	@SuppressWarnings("unchecked")
	public Class getObjectType() {
		return Step.class;
	}

	/**
	 * Returns true by default, but in most cases a {@link Step} should not be
	 * treated as thread safe. Clients are recommended to create a new step for
	 * each job execution.
	 * 
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	public boolean isSingleton() {
		return this.singleton;
	}

	/**
	 * Public setter for the singleton flag.
	 * @param singleton the value to set. Defaults to true.
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

}