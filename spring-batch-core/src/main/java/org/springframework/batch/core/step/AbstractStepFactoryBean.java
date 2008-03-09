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
package org.springframework.batch.core.step;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * Base class for factory beans for {@link ItemOrientedStep}. Ensures that all
 * the mandatory properties are set, and provides basic support for the
 * {@link Step} interface responsibilities like start limit.
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

	private JobRepository jobRepository;

	private boolean singleton = true;

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
		Assert.notNull(jobRepository, "JobRepository must be provided");
		Assert.notNull(transactionManager, "TransactionManager must be provided");

		step.setItemHandler(new SimpleItemHandler(itemReader, itemWriter));
		step.setTransactionManager(transactionManager);
		step.setJobRepository(jobRepository);
		step.setStartLimit(startLimit);
		step.setAllowStartIfComplete(allowStartIfComplete);

	}

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