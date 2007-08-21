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

import org.springframework.batch.core.tasklet.Recoverable;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemProvider;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.synch.RepeatSynchronizationManager;
import org.springframework.batch.retry.RetryOperations;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.callback.ItemProviderRetryCallback;
import org.springframework.batch.retry.policy.ItemProviderRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A concrete implementation of the {@link Tasklet} interface that provides
 * functionality for 'split processing'. This type of processing is
 * characterised by separating the reading and processing of batch data into two
 * separate classes: ItemProvider and DataProcessor. The ItemProvider class
 * provides a solid means for re-usability and enforces good architecture
 * practices. Because an object *must* be returned by the {@link ItemProvider}
 * to continue processing, (returning null indicates processing should end) a
 * developer is forced to read in all relevant data, place it into a domain
 * object, and return that object. The {@link ItemProcessor} will then use this
 * object for calculations and output.<br/>
 * 
 * If a {@link RetryPolicy} is provided it will be used to construct a stateful
 * retry around the {@link ItemProcessor}, delegating recover and identity
 * concerns to the {@link ItemProvider}. In this case clients of this class do
 * not need to take any additional action at runtime to take advantage of the
 * retry and recovery, provided the {@link #execute()} method is called again
 * with the {@link ItemProvider} in the same state (normally this would be the
 * case because a transaction would have rolled back and the item would be
 * represented).<br/>
 * 
 * If neither a {@link RetryPolicy} nor a {@link RetryOperations} is provided
 * then the {@link Recoverable} interface can be used to attempt to recover
 * immediately (with no retry) from a processing error. Clients of this class
 * must call {@link Recoverable#recover(Throwable)} directly, which is simply
 * delegated to {@link ItemProvider#recover(Object, Throwable)}.
 * 
 * @see ItemProvider
 * @see ItemProcessor
 * @see RetryPolicy
 * @see Recoverable
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @author Robert Kasanicky
 * 
 */
public class ItemProviderProcessTasklet implements Tasklet, Recoverable, Skippable, StatisticsProvider,
		InitializingBean {

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

	/**
	 * Attribute key in the surrounding {@link RepeatContext} for the current
	 * item being processed. Needed to provide recoverable behaviour if
	 * {@link RetryOperations} are not provided.
	 */
	private static final String ITEM_KEY = ItemProviderProcessTasklet.class + ".ITEM";

	private RetryPolicy retryPolicy = null;

	private RetryOperations retryOperations = null;

	protected ItemProvider itemProvider;

	protected ItemProcessor itemProcessor;

	/**
	 * Check mandatory properties (provider and processor), and ensure that only
	 * one (or neither) of {@link RetryPolicy} or {@link RetryOperations} is
	 * provided.
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(itemProvider, "ItemProvider must be provided");
		Assert.notNull(itemProcessor, "ItemProcessor must be provided");
		Assert.state(!(retryPolicy != null && retryOperations != null),
				"Either RetryOperations or RetryPolicy can be provided, but not both.");
		if (retryPolicy != null) {
			RetryTemplate template = new RetryTemplate();
			template.setRetryPolicy(new ItemProviderRetryPolicy(retryPolicy));
			retryOperations = template;
		}
	}

	/**
	 * Read from the {@link ItemProvider} and process (if not null) with the
	 * {@link ItemProcessor}. The call to {@link ItemProcessor} is wrapped in a
	 * retry, if either a {@link RetryPolicy} or a {@link RetryOperations} is
	 * provided.
	 * 
	 * @see org.springframework.batch.core.tasklet.Tasklet#execute()
	 */
	public ExitStatus execute() throws Exception {
		if (retryOperations != null) {
			return new ExitStatus(retryOperations.execute(new ItemProviderRetryCallback(itemProvider, itemProcessor)) != null);
		}
		else {
			Object data = itemProvider.next();
			if (data == null) {
				return ExitStatus.FINISHED;
			}
			RepeatContext context = RepeatSynchronizationManager.getContext();
			Assert.state(context != null,
					"No context available: you probably need to use this class inside a batch operation.");
			context.setAttribute(ITEM_KEY, data);
			itemProcessor.process(data);
			// No exception so clear context (we can't recover directly because
			// the current transaction is going to roll back)
			context.removeAttribute(ITEM_KEY);
			return ExitStatus.CONTINUABLE;
		}
	}

	/**
	 * Call out to the provider for recovery step.
	 * 
	 * @see org.springframework.batch.core.tasklet.Recoverable#recover(java.lang.Throwable)
	 */
	public void recover(Throwable cause) {
		RepeatContext context = RepeatSynchronizationManager.getContext();
		Assert.state(context != null,
				"No context available: you probably need to use this class inside a batch operation.");

		try {
			Object data = context.getAttribute(ITEM_KEY);
			itemProvider.recover(data, cause);
		}
		finally {
			context.removeAttribute(ITEM_KEY);
		}
	}

	/**
	 * @param itemProvider
	 */
	public void setItemProvider(ItemProvider itemProvider) {
		this.itemProvider = itemProvider;
	}

	/**
	 * @param moduleProcessor
	 */
	public void setItemProcessor(ItemProcessor moduleProcessor) {
		this.itemProcessor = moduleProcessor;
	}

	/**
	 * If the provider and / or processor are {@link Skippable} then delegate to
	 * them in that order.
	 * 
	 * @see org.springframework.batch.io.Skippable#skip()
	 */
	public void skip() {
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
			Properties props = ((StatisticsProvider) this.itemProcessor).getStatistics();
			if (!stats.isEmpty()) {
				stats = prependKeys(stats, props, PROVIDER_STATISTICS_PREFIX, PROCESSOR_STATISTICS_PREFIX);
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
	private Properties prependKeys(Properties props1, Properties props2, String prefix1, String prefix2) {
		Properties result = new Properties();
		Set duplicates = new HashSet();
		for (Iterator iterator = props1.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			if (props2.containsKey(key)) {
				duplicates.add(key);
				continue;
			}
			result.setProperty(key, value);
		}
		for (Iterator iterator = props2.entrySet().iterator(); iterator.hasNext();) {
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
			result.setProperty(prefix1+key, props1.getProperty(key));
			result.setProperty(prefix2+key, props2.getProperty(key));
		}
		return result;
	}

	/**
	 * Public setter for the retryPolicy.
	 * 
	 * @param retyPolicy the retryPolicy to set
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
	}

	/**
	 * Public setter for the retryOperations.
	 * 
	 * @param retryOperations the retryOperations to set
	 */
	public void setRetryOperations(RetryOperations retryOperations) {
		this.retryOperations = retryOperations;
	}
}
