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

package org.springframework.batch.core.configuration.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.item.FaultTolerantStepFactoryBean;
import org.springframework.batch.core.step.item.SimpleStepFactoryBean;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.batch.retry.RetryListener;
import org.springframework.batch.retry.policy.MapRetryContextCache;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.util.Assert;

/**
 * This {@link FactoryBean} is used by the batch namespace parser to create
 * {@link Step} objects. Stores all of the properties that are configurable on
 * the &lt;step/&gt; (and its inner &lt;tasklet/&gt;). Based on which properties
 * are configured, the {@link #getObject()} method will delegate to the
 * appropriate class for generating the {@link Step}.
 * 
 * @author Dan Garrette
 * @since 2.0
 * @see SimpleStepFactoryBean
 * @see FaultTolerantStepFactoryBean
 * @see TaskletStep
 */
class StepParserStepFactoryBean<I, O> implements FactoryBean, BeanNameAware {

	//
	// Step Attributes
	//
	private String name;

	//
	// Tasklet Attributes
	//
	private Boolean allowStartIfComplete;

	private JobRepository jobRepository;

	private Integer startLimit;

	private Tasklet tasklet;

	private PlatformTransactionManager transactionManager;

	//
	// Tasklet Elements
	//
	private StepListener[] listeners;

	private Collection<Class<? extends Throwable>> noRollbackExceptionClasses;

	private int transactionTimeout = DefaultTransactionAttribute.TIMEOUT_DEFAULT;

	private Propagation propagation;

	private Isolation isolation;

	//
	// Chunk Attributes
	//
	private Integer cacheCapacity;

	private CompletionPolicy chunkCompletionPolicy;

	private Integer commitInterval;

	private Boolean isReaderTransactionalQueue;

	private Integer retryLimit;

	private Integer skipLimit;

	private TaskExecutor taskExecutor;

	private Integer throttleLimit;

	private ItemReader<? extends I> itemReader;

	private ItemProcessor<? super I, ? extends O> itemProcessor;

	private ItemWriter<? super O> itemWriter;

	//
	// Chunk Elements
	//
	private RetryListener[] retryListeners;

	private Collection<Class<? extends Throwable>> skippableExceptionClasses;

	private Collection<Class<? extends Throwable>> retryableExceptionClasses;

	private Collection<Class<? extends Throwable>> fatalExceptionClasses;

	private ItemStream[] streams;

	//
	// Additional
	//
	private boolean hasChunkElement = false;

	/**
	 * Create a {@link Step} from the configuration provided.
	 * 
	 * @see FactoryBean#getObject()
	 */
	public final Object getObject() throws Exception {
		if (hasChunkElement) {
			Assert.isNull(tasklet, "Step [" + name
					+ "] has both a <chunk/> element and a 'ref' attribute  referencing a Tasklet.");

			if (isFaultTolerant()) {
				FaultTolerantStepFactoryBean<I, O> fb = new FaultTolerantStepFactoryBean<I, O>();
				configureSimple(fb);
				configureFaultTolerant(fb);
				return fb.getObject();
			}
			else {
				validateSimpleStep();
				SimpleStepFactoryBean<I, O> fb = new SimpleStepFactoryBean<I, O>();
				configureSimple(fb);
				return fb.getObject();
			}
		}
		else if (tasklet != null) {
			TaskletStep ts = new TaskletStep();
			configureTaskletStep(ts);
			return ts;
		}
		else {
			throw new IllegalStateException("Step [" + name
					+ "] has neither a <chunk/> element nor a 'ref' attribute referencing a Tasklet.");
		}
	}

	private void configureSimple(SimpleStepFactoryBean<I, O> fb) {
		if (name != null) {
			fb.setBeanName(name);
		}
		if (allowStartIfComplete != null) {
			fb.setAllowStartIfComplete(allowStartIfComplete);
		}
		if (jobRepository != null) {
			fb.setJobRepository(jobRepository);
		}
		if (startLimit != null) {
			fb.setStartLimit(startLimit);
		}
		if (transactionManager != null) {
			fb.setTransactionManager(transactionManager);
		}
		if (listeners != null) {
			fb.setListeners(listeners);
		}
		if (transactionTimeout >= 0) {
			fb.setTransactionTimeout(transactionTimeout);
		}
		if (propagation != null) {
			fb.setPropagation(propagation);
		}
		if (isolation != null) {
			fb.setIsolation(isolation);
		}

		if (chunkCompletionPolicy != null) {
			fb.setChunkCompletionPolicy(chunkCompletionPolicy);
		}
		if (commitInterval != null) {
			fb.setCommitInterval(commitInterval);
		}
		if (taskExecutor != null) {
			fb.setTaskExecutor(taskExecutor);
		}
		if (throttleLimit != null) {
			fb.setThrottleLimit(throttleLimit);
		}
		if (itemReader != null) {
			fb.setItemReader(itemReader);
		}
		if (itemProcessor != null) {
			fb.setItemProcessor(itemProcessor);
		}
		if (itemWriter != null) {
			fb.setItemWriter(itemWriter);
		}

		if (streams != null) {
			fb.setStreams(streams);
		}
	}

	private void configureFaultTolerant(FaultTolerantStepFactoryBean<I, O> fb) {
		if (cacheCapacity != null) {
			fb.setCacheCapacity(cacheCapacity);
		}
		if (isReaderTransactionalQueue != null) {
			fb.setIsReaderTransactionalQueue(isReaderTransactionalQueue);
		}
		if (retryLimit != null) {
			fb.setRetryLimit(retryLimit);
		}
		if (skipLimit != null) {
			fb.setSkipLimit(skipLimit);
		}

		if (retryListeners != null) {
			fb.setRetryListeners(retryListeners);
		}
		if (skippableExceptionClasses != null) {
			fb.setSkippableExceptionClasses(skippableExceptionClasses);
		}
		if (retryableExceptionClasses != null) {
			fb.setRetryableExceptionClasses(retryableExceptionClasses);
		}
		if (fatalExceptionClasses != null) {
			fb.setFatalExceptionClasses(fatalExceptionClasses);
		}
		if (noRollbackExceptionClasses != null) {
			fb.setNoRollbackExceptionClasses(noRollbackExceptionClasses);
		}
	}

	private void configureTaskletStep(TaskletStep ts) {
		if (name != null) {
			ts.setName(name);
		}
		if (allowStartIfComplete != null) {
			ts.setAllowStartIfComplete(allowStartIfComplete);
		}
		if (jobRepository != null) {
			ts.setJobRepository(jobRepository);
		}
		if (startLimit != null) {
			ts.setStartLimit(startLimit);
		}
		if (tasklet != null) {
			ts.setTasklet(tasklet);
		}
		if (transactionManager != null) {
			ts.setTransactionManager(transactionManager);
		}
		if (listeners != null) {
			int i = 0;
			StepExecutionListener[] newListeners = new StepExecutionListener[listeners.length];
			for (StepListener listener : listeners) {
				newListeners[i++] = (StepExecutionListener) listener;
			}
			ts.setStepExecutionListeners((StepExecutionListener[]) newListeners);
		}
		if (transactionTimeout >= 0 || propagation != null || isolation != null) {
			DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
			attribute.setPropagationBehavior(propagation.value());
			attribute.setIsolationLevel(isolation.value());
			attribute.setTimeout(transactionTimeout);
			ts.setTransactionAttribute(new DefaultTransactionAttribute(attribute) {

				/**
				 * Ignore the default behaviour and rollback on all exceptions
				 * that bubble up to the tasklet level. The tasklet has to deal
				 * with the rollback rules internally.
				 */
				@Override
				public boolean rollbackOn(Throwable ex) {
					return true;
				}

			});
		}
	}

	private void validateSimpleStep() {
		PropertyNamePair[] notPermitted = new PropertyNamePair[] {
				new PropertyNamePair(retryListeners, "retry-listeners"),
				new PropertyNamePair(skippableExceptionClasses, "skippable-exception-classes"),
				new PropertyNamePair(retryableExceptionClasses, "retryable-exception-classes"),
				new PropertyNamePair(fatalExceptionClasses, "fatal-exception-classes") };

		List<String> wrong = new ArrayList<String>();
		for (PropertyNamePair field : notPermitted) {
			if (field.getProperty() != null) {
				wrong.add(field.getName());
			}
		}
		if (!wrong.isEmpty()) {
			throw new IllegalArgumentException("The field" + (wrong.size() > 1 ? "s " : " ") + wrong
					+ (wrong.size() == 1 ? " is" : " are") + " not permitted on the simple step [" + name + "].  "
					+ (wrong.size() == 1 ? "It" : "They") + " can only be specified for fault-tolerant "
					+ "configurations providing skip-limit, retry-limit, or cache-capacity");
		}
	}

	private static class PropertyNamePair {
		private Object property;

		private String name;

		public PropertyNamePair(Object property, String name) {
			super();
			this.property = property;
			this.name = name;
		}

		public Object getProperty() {
			return property;
		}

		public String getName() {
			return name;
		}
	}

	private boolean isFaultTolerant() {
		return isPositive(skipLimit) || isPositive(retryLimit) || isPositive(cacheCapacity)
				|| isTrue(isReaderTransactionalQueue);
	}

	private boolean isTrue(Boolean b) {
		return b != null && b.booleanValue();
	}

	private boolean isPositive(Integer n) {
		return n != null && n > 0;
	}

	public Class<TaskletStep> getObjectType() {
		return TaskletStep.class;
	}

	public boolean isSingleton() {
		return false;
	}

	// =========================================================
	// Step Attributes
	// =========================================================

	/**
	 * Set the bean name property, which will become the name of the
	 * {@link Step} when it is created.
	 * 
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		if (this.name == null) {
			this.name = name;
		}
	}

	// =========================================================
	// Tasklet Attributes
	// =========================================================

	/**
	 * Public setter for the flag to indicate that the step should be replayed
	 * on a restart, even if successful the first time.
	 * 
	 * @param allowStartIfComplete the shouldAllowStartIfComplete to set
	 */
	public void setAllowStartIfComplete(boolean allowStartIfComplete) {
		this.allowStartIfComplete = allowStartIfComplete;

	}

	/**
	 * @return jobRepository
	 */
	public JobRepository getJobRepository() {
		return jobRepository;
	}

	/**
	 * Public setter for {@link JobRepository}.
	 * 
	 * @param jobRepository
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * The number of times that the step should be allowed to start
	 * 
	 * @param startLimit
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
	}

	/**
	 * A preconfigured {@link Tasklet} to use.
	 * 
	 * @param tasklet
	 */
	public void setTasklet(Tasklet tasklet) {
		this.tasklet = tasklet;
	}

	/**
	 * @return transactionManager
	 */
	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	/**
	 * @param transactionManager the transaction manager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	// =========================================================
	// Tasklet Elements
	// =========================================================

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
	 * Exception classes that may not cause a rollback if encountered in the
	 * right place.
	 * 
	 * @param noRollbackExceptionClasses the noRollbackExceptionClasses to set
	 */
	public void setNoRollbackExceptionClasses(Collection<Class<? extends Throwable>> noRollbackExceptionClasses) {
		this.noRollbackExceptionClasses = noRollbackExceptionClasses;
	}

	/**
	 * @param transactionTimeout the transactionTimeout to set
	 */
	public void setTransactionTimeout(int transactionTimeout) {
		this.transactionTimeout = transactionTimeout;
	}

	/**
	 * @param isolation the isolation to set
	 */
	public void setIsolation(Isolation isolation) {
		this.isolation = isolation;
	}

	/**
	 * @param propagation the propagation to set
	 */
	public void setPropagation(Propagation propagation) {
		this.propagation = propagation;
	}

	// =========================================================
	// Chunk Attributes
	// =========================================================

	/**
	 * Public setter for the capacity of the cache in the retry policy. If more
	 * items than this fail without being skipped or recovered an exception will
	 * be thrown. This is to guard against inadvertent infinite loops generated
	 * by item identity problems.<br/>
	 * 
	 * The default value should be high enough and more for most purposes. To
	 * breach the limit in a single-threaded step typically you have to have
	 * this many failures in a single transaction. Defaults to the value in the
	 * {@link MapRetryContextCache}.<br/>
	 * 
	 * @param cacheCapacity the cache capacity to set (greater than 0 else
	 *        ignored)
	 */
	public void setCacheCapacity(int cacheCapacity) {
		this.cacheCapacity = cacheCapacity;
	}

	/**
	 * Public setter for the {@link CompletionPolicy} applying to the chunk
	 * level. A transaction will be committed when this policy decides to
	 * complete. Defaults to a {@link SimpleCompletionPolicy} with chunk size
	 * equal to the commitInterval property.
	 * 
	 * @param chunkCompletionPolicy the chunkCompletionPolicy to set
	 */
	public void setChunkCompletionPolicy(CompletionPolicy chunkCompletionPolicy) {
		this.chunkCompletionPolicy = chunkCompletionPolicy;
	}

	/**
	 * Set the commit interval. Either set this or the chunkCompletionPolicy but
	 * not both.
	 * 
	 * @param commitInterval 1 by default
	 */
	public void setCommitInterval(int commitInterval) {
		this.commitInterval = commitInterval;
	}

	/**
	 * Flag to signal that the reader is transactional (usually a JMS consumer)
	 * so that items are re-presented after a rollback. The default is false and
	 * readers are assumed to be forward-only.
	 * 
	 * @param isReaderTransactionalQueue the value of the flag
	 */
	public void setIsReaderTransactionalQueue(boolean isReaderTransactionalQueue) {
		this.isReaderTransactionalQueue = isReaderTransactionalQueue;
	}

	/**
	 * Public setter for the retry limit. Each item can be retried up to this
	 * limit. Note this limit includes the initial attempt to process the item,
	 * therefore <code>retryLimit == 1</code> by default.
	 * 
	 * @param retryLimit the retry limit to set, must be greater or equal to 1.
	 */
	public void setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
	}

	/**
	 * Public setter for a limit that determines skip policy. If this value is
	 * positive then an exception in chunk processing will cause the item to be
	 * skipped and no exception propagated until the limit is reached. If it is
	 * zero then all exceptions will be propagated from the chunk and cause the
	 * step to abort.
	 * 
	 * @param skipLimit the value to set. Default is 0 (never skip).
	 */
	public void setSkipLimit(int skipLimit) {
		this.skipLimit = skipLimit;
	}

	/**
	 * Public setter for the {@link TaskExecutor}. If this is set, then it will
	 * be used to execute the chunk processing inside the {@link Step}.
	 * 
	 * @param taskExecutor the taskExecutor to set
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Public setter for the throttle limit. This limits the number of tasks
	 * queued for concurrent processing to prevent thread pools from being
	 * overwhelmed. Defaults to
	 * {@link TaskExecutorRepeatTemplate#DEFAULT_THROTTLE_LIMIT}.
	 * 
	 * @param throttleLimit the throttle limit to set.
	 */
	public void setThrottleLimit(Integer throttleLimit) {
		this.throttleLimit = throttleLimit;
	}

	/**
	 * @param itemReader the {@link ItemReader} to set
	 */
	public void setItemReader(ItemReader<? extends I> itemReader) {
		this.itemReader = itemReader;
	}

	/**
	 * @param itemProcessor the {@link ItemProcessor} to set
	 */
	public void setItemProcessor(ItemProcessor<? super I, ? extends O> itemProcessor) {
		this.itemProcessor = itemProcessor;
	}

	/**
	 * @param itemWriter the {@link ItemWriter} to set
	 */
	public void setItemWriter(ItemWriter<? super O> itemWriter) {
		this.itemWriter = itemWriter;
	}

	// =========================================================
	// Chunk Elements
	// =========================================================

	/**
	 * Public setter for the {@link RetryListener}s.
	 * 
	 * @param retryListeners the {@link RetryListener}s to set
	 */
	public void setRetryListeners(RetryListener... retryListeners) {
		this.retryListeners = retryListeners;
	}

	/**
	 * Public setter for exception classes that when raised won't crash the job
	 * but will result in transaction rollback and the item which handling
	 * caused the exception will be skipped.
	 * 
	 * @param exceptionClasses
	 */
	public void setSkippableExceptionClasses(Collection<Class<? extends Throwable>> exceptionClasses) {
		this.skippableExceptionClasses = exceptionClasses;
	}

	/**
	 * Public setter for exception classes that will retry the item when raised.
	 * 
	 * @param retryableExceptionClasses the retryableExceptionClasses to set
	 */
	public void setRetryableExceptionClasses(Collection<Class<? extends Throwable>> retryableExceptionClasses) {
		this.retryableExceptionClasses = retryableExceptionClasses;
	}

	/**
	 * Public setter for exception classes that should cause immediate failure.
	 * 
	 * @param fatalExceptionClasses
	 */
	public void setFatalExceptionClasses(Collection<Class<? extends Throwable>> fatalExceptionClasses) {
		this.fatalExceptionClasses = fatalExceptionClasses;
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

	// =========================================================
	// Additional
	// =========================================================

	/**
	 * @param hasChunkElement
	 */
	public void setHasChunkElement(boolean hasChunkElement) {
		this.hasChunkElement = hasChunkElement;
	}

}
