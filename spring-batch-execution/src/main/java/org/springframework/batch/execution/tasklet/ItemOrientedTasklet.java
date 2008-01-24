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

package org.springframework.batch.execution.tasklet;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.callback.ItemReaderRetryCallback;
import org.springframework.batch.retry.policy.ItemReaderRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A concrete implementation of the {@link Tasklet} interface that provides
 * 'split processing'. This type of processing is characterized by separating
 * the reading and processing of batch data into two separate classes:
 * {@link ItemReader} and {@link ItemProcessor}. The {@link ItemReader} class provides a solid means
 * for re-usability and enforces good architecture practices. Because an object
 * <em>must</em> be returned by the {@link ItemReader} to continue
 * processing, (returning null indicates processing should end) a developer is
 * forced to read in all relevant data, place it into a domain object, and
 * return that object. The {@link ItemProcessor} will then use this object for
 * calculations and output.<br/>
 * 
 * If a {@link RetryPolicy} is provided it will be used to construct a stateful
 * retry around the {@link ItemProcessor}, delegating identity concerns to the
 * {@link ItemReader} and recovery concerns to the {@link ItemRecoverer} (if
 * present). In this case clients of this class do not need to take any
 * additional action at runtime to take advantage of the retry and recovery,
 * provided that when the {@link #execute()} method is called again the same
 * item is eventually re-presented (normally this would be the case because a
 * transaction would have rolled back and the {@link ItemReader} would go back
 * to its previous state).<br/>
 * 
 * If a {@link RetryPolicy} is not provided then the {@link ItemRecoverer} can
 * be used to attempt to recover immediately (with no retry) from a processing
 * error. Clients of this class should ensure that the recovery takes place in a
 * separate transaction (e.g. with propagation REQUIRES_NEW) if necessary. This
 * can be achieved by injecting an {@link ItemRecoverer} that has a
 * transactional recover method.
 * 
 * @see ItemReader
 * @see ItemProcessor
 * @see RetryPolicy
 * @see Recoverable
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @author Robert Kasanicky
 * 
 */
public class ItemOrientedTasklet implements Tasklet, Skippable,
		StatisticsProvider, InitializingBean {

	/**
	 * Prefix added to statistics keys from processor if needed to avoid
	 * ambiguity between provider and processor.
	 */
	public static final String PROCESSOR_STATISTICS_PREFIX = "processor.";

	/**
	 * Prefix added to statistics keys from provider if needed to avoid
	 * ambiguity between provider and processor.
	 */
	public static final String PROVIDER_STATISTICS_PREFIX = "provider.";

	private RetryPolicy retryPolicy = null;

	protected ItemReader itemProvider;

	protected ItemProcessor itemProcessor;

	private ItemRecoverer itemRecoverer;

	private RetryTemplate template = new RetryTemplate();

	private ItemReaderRetryCallback retryCallback;

	/**
	 * Check mandatory properties (provider and processor).
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(itemProvider, "ItemReader must be provided");
		Assert.notNull(itemProcessor, "ItemProcessor must be provided");

		if (itemRecoverer == null && (itemProvider instanceof ItemRecoverer)) {
			itemRecoverer = (ItemRecoverer) itemProvider;
		}

		ItemReaderRetryPolicy itemProviderRetryPolicy = new ItemReaderRetryPolicy(
				retryPolicy);
		template.setRetryPolicy(itemProviderRetryPolicy);

		if (retryPolicy != null) {
			retryCallback = new ItemReaderRetryCallback(itemProvider,
					itemProcessor);
			retryCallback.setRecoverer(itemRecoverer);
		}

	}

	/**
	 * Read from the {@link ItemReader} and process (if not null) with the
	 * {@link ItemProcessor}. The call to {@link ItemProcessor} is wrapped in a
	 * stateful retry, if a {@link RetryPolicy} is provided. The
	 * {@link ItemRecoverer} is used (if provided) in the case of an exception
	 * to apply alternate processing to the item. If the stateful retry is in
	 * place then the recovery will happen in the next transaction
	 * automatically, otherwise it might be necessary for clients to make the
	 * recover method transactional with appropriate propagation behaviour
	 * (probably REQUIRES_NEW because the call will happen in the context of a
	 * transaction that is about to rollback).
	 * 
	 * @see org.springframework.batch.core.tasklet.Tasklet#execute()
	 */
	public ExitStatus execute() throws Exception {

		if (retryCallback == null) {
			Object item = itemProvider.read();
			if (item == null) {
				return ExitStatus.FINISHED;
			}
			try {
				itemProcessor.process(item);
			} catch (Exception e) {
				if (itemRecoverer != null) {
					itemRecoverer.recover(item, e);
				}
				// Re-throw the exception so that the surrounding transaction
				// rolls back if there is one
				throw e;
			}
			return ExitStatus.CONTINUABLE;
		}

		return new ExitStatus(template.execute(retryCallback) != null);

	}

	/**
	 * @param itemProvider
	 */
	public void setItemReader(ItemReader itemProvider) {
		this.itemProvider = itemProvider;
	}

	/**
	 * @param moduleProcessor
	 */
	public void setItemProcessor(ItemProcessor moduleProcessor) {
		this.itemProcessor = moduleProcessor;
	}

	/**
	 * Setter for injecting optional recovery handler.
	 * 
	 * @param itemRecoverer
	 */
	public void setItemRecoverer(ItemRecoverer itemRecoverer) {
		this.itemRecoverer = itemRecoverer;
	}

	/**
	 * Mark the current item as skipped if possible. If there is a retry policy
	 * in action there is no need to take any action now because it will be
	 * covered by the retry in the next transaction. Otherwise if the provider
	 * and / or processor are {@link Skippable} then delegate to them in that
	 * order.
	 * 
	 * @see org.springframework.batch.io.Skippable#skip()
	 */
	public void skip() {
		if (retryCallback != null) {
			// No need to skip because the recoverer will take any action
			// necessary.
			return;
		}
		if (this.itemProvider instanceof Skippable) {
			((Skippable) this.itemProvider).skip();
		}
		if (this.itemProcessor instanceof Skippable) {
			((Skippable) this.itemProcessor).skip();
		}
	}

	/**
	 * If the provider and / or processor are {@link StatisticsProvider} then
	 * delegate to them in that order. If they both implement
	 * {@link StatisticsProvider} then the property keys are prepended with
	 * special prefixes to avoid potential ambiguity. The prefixes are only
	 * prepended in the case of a duplicate key shared between provider and
	 * processor.
	 * 
	 * @see org.springframework.batch.io.Skippable#skip()
	 */
	public Properties getStatistics() {
		Properties stats = new Properties();
		if (this.itemProvider instanceof StatisticsProvider) {
			stats = ((StatisticsProvider) this.itemProvider).getStatistics();
		}
		if (this.itemProcessor instanceof StatisticsProvider) {
			Properties props = ((StatisticsProvider) this.itemProcessor)
					.getStatistics();
			if (!stats.isEmpty()) {
				stats = prependKeys(stats, props, PROVIDER_STATISTICS_PREFIX,
						PROCESSOR_STATISTICS_PREFIX);
			} else {
				stats.putAll(props);
			}
		}
		return stats;
	}

	/**
	 * @param props1
	 * @param string
	 * @return
	 */
	private Properties prependKeys(Properties props1, Properties props2,
			String prefix1, String prefix2) {
		Properties result = new Properties();
		Set duplicates = new HashSet();
		for (Iterator iterator = props1.entrySet().iterator(); iterator
				.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			if (props2.containsKey(key)) {
				duplicates.add(key);
				continue;
			}
			result.setProperty(key, value);
		}
		for (Iterator iterator = props2.entrySet().iterator(); iterator
				.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			if (duplicates.contains(key)) {
				continue;
			}
			result.setProperty(key, value);
		}
		for (Iterator iterator = duplicates.iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			result.setProperty(prefix1 + key, props1.getProperty(key));
			result.setProperty(prefix2 + key, props2.getProperty(key));
		}
		return result;
	}

	/**
	 * Public setter for the retryPolicy.
	 * 
	 * @param retyPolicy
	 *            the retryPolicy to set
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
	}
}
