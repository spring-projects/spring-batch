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
package org.springframework.batch.execution.step.support;

import org.springframework.batch.core.domain.Step;
import org.springframework.batch.execution.step.ItemOrientedStep;
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.callback.ItemReaderRetryCallback;
import org.springframework.batch.retry.policy.ItemReaderRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;

/**
 * Factory bean for step that executes its item processing with a stateful
 * retry. Failed items are never skipped, but always cause a rollback. Before a
 * rollback, the {@link Step} makes a record of the failed item, caching it
 * under a key given by the {@link ItemKeyGenerator}. Then when it is
 * re-presented by the {@link ItemReader} it is recognised and retried up to a
 * limit given by the {@link RetryPolicy}. When the retry is exhausted instead
 * of the item being skipped it is handled by an {@link ItemRecoverer}.<br/>
 * 
 * TODO: checking for null retry callback is a sucky way of determining if a
 * stateful retry has been requested.
 * 
 * @author Dave Syer
 * 
 */
public class StatefulRetryStepFactoryBean extends DefaultStepFactoryBean {

	private RetryPolicy retryPolicy;

	private ItemKeyGenerator itemKeyGenerator;

	/**
	 * Public setter for the {@link RetryPolicy}.
	 * @param retryPolicy the {@link RetryPolicy} to set
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
	}

	/**
	 * Public setter for the {@link ItemKeyGenerator} which will be used to
	 * cache failed items between transactions. If it is not injected but the
	 * reader or writer implement {@link ItemKeyGenerator}, one of those will
	 * be used instead (preferring the reader to the writer if both would be
	 * appropriate). If neither can be used, then the default will be to just
	 * use the item itself as a cache key.
	 * 
	 * @param itemKeyGenerator the {@link ItemKeyGenerator} to set
	 */
	public void setItemKeyGenerator(ItemKeyGenerator itemKeyGenerator) {
		this.itemKeyGenerator = itemKeyGenerator;
	}

	/**
	 * @param step
	 * 
	 */
	protected void applyConfiguration(ItemOrientedStep step) {

		// Ensure exception handler always rethrows. N.B. no skips ever actually
		// take place.
		if (retryPolicy != null) {
			// TODO: actually we need to co-ordinate the retry policy with the
			// exception handler limit, so this is a hack for now.
			super.setSkipLimit(Integer.MAX_VALUE);
		}

		super.applyConfiguration(step);

		if (retryPolicy != null) {
			ItemReaderRetryCallback retryCallback = new ItemReaderRetryCallback(getItemReader(), getKeyGenerator(),
					getItemWriter());
			ItemReaderRetryPolicy itemProviderRetryPolicy = new ItemReaderRetryPolicy(retryPolicy);
			RetryTemplate retryTemplate = new RetryTemplate();
			retryTemplate.setRetryPolicy(itemProviderRetryPolicy);
			KitchenSinkItemProcessor itemProcessor = (KitchenSinkItemProcessor) getItemProcessor();
			itemProcessor.setRetryOperations(retryTemplate);
			itemProcessor.setRetryCallback(retryCallback);
		}

	}

	/**
	 * @return an {@link ItemKeyGenerator} or null if none is found.
	 */
	private ItemKeyGenerator getKeyGenerator() {

		if (itemKeyGenerator != null) {
			return itemKeyGenerator;
		}
		if (getItemReader() instanceof ItemKeyGenerator) {
			return (ItemKeyGenerator) getItemReader();
		}
		if (getItemWriter() instanceof ItemKeyGenerator) {
			return (ItemKeyGenerator) getItemWriter();
		}
		return null;

	}

}
