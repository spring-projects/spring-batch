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

import org.springframework.batch.core.domain.ItemSkipPolicy;
import org.springframework.batch.core.domain.StepContribution;
import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.ExitStatus;

/**
 * @author Dave Syer
 * 
 */
public class ItemSkipPolicyItemHandler extends SimpleItemHandler {

	private ItemSkipPolicy itemSkipPolicy = new NeverSkipItemSkipPolicy();

	/**
	 * @param itemReader
	 * @param itemWriter
	 */
	public ItemSkipPolicyItemHandler(ItemReader itemReader, ItemWriter itemWriter) {
		super(itemReader, itemWriter);
	}

	/**
	 * @param itemSkipPolicy
	 */
	public void setItemSkipPolicy(ItemSkipPolicy itemSkipPolicy) {
		this.itemSkipPolicy = itemSkipPolicy;
	}

	/**
	 * Execute the business logic, delegating to the reader and writer.
	 * Subclasses could extend the behaviour as long as they always return the
	 * value of this method call in their superclass.<br/>
	 * 
	 * Read from the {@link ItemReader} and process (if not null) with the
	 * {@link ItemWriter}.<br/>
	 * 
	 * If there is an exception and the reader or writer implements
	 * {@link Skippable} then the skip method is called.
	 * 
	 * @param contribution the current step
	 * @return {@link ExitStatus#CONTINUABLE} if there is more processing to do
	 * @throws Exception if there is an error
	 */
	public ExitStatus handle(StepContribution contribution) throws Exception {
		ExitStatus exitStatus = ExitStatus.CONTINUABLE;

		try {

			exitStatus = super.handle(contribution);

		}
		catch (Exception e) {

			if (itemSkipPolicy.shouldSkip(e, contribution.getSkipCount())) {
				contribution.incrementSkipCount();
				skip();
			}
			else {
				// Rethrow so that outer transaction is rolled back properly
				throw e;
			}

		}

		return exitStatus;
	}

	/**
	 * Mark the current item as skipped if possible. If the reader and / or
	 * writer are {@link Skippable} then delegate to them in that order.
	 * 
	 * @see org.springframework.batch.io.Skippable#skip()
	 */
	private void skip() {
		if (getItemReader() instanceof Skippable) {
			((Skippable) getItemReader()).skip();
		}
		if (getItemWriter() instanceof Skippable) {
			((Skippable) getItemWriter()).skip();
		}
	}

}
