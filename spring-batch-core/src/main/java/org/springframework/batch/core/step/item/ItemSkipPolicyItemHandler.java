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

import org.springframework.batch.core.ItemSkipPolicy;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.Skippable;

/**
 * {@link ItemHandler} that implements skip behavior. It delegates to
 * {@link #itemSkipPolicy} to decide whether skip should be called or not.
 * 
 * If exception is thrown while reading the item, skip is called on the
 * {@link ItemReader}. If exception is thrown while writing the item, skip is
 * called on both {@link ItemReader} and {@link ItemWriter}.
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
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
	 * Tries to read the item from the reader, in case exception is thrown calls
	 * skip on the reader (if skipPolicy decides it is appropriate) before
	 * rethrowing the exception.
	 * 
	 * @param contribution current StepContribution holding skipped items count
	 * @return next item for processing
	 */
	protected Object read(StepContribution contribution) throws Exception {
		try {
			return getItemReader().read();
		}
		catch (Exception e) {
			if (itemSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
				contribution.incrementSkipCount();
				if (getItemReader() instanceof Skippable) {
					((Skippable) getItemReader()).skip();
				}
			}
			throw e;
		}
	}

	/**
	 * Tries to write the item using the writer, in case exception is thrown
	 * calls skip on both reader and writer (if skipPolicy decides it is
	 * appropriate) before rethrowing the exception.
	 * 
	 * @param item item to write
	 * @param contribution current StepContribution holding skipped items count
	 */
	protected void write(Object item, StepContribution contribution) throws Exception {
		try {
			getItemWriter().write(item);
		}
		catch (Exception e) {
			if (itemSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
				contribution.incrementSkipCount();
				if (getItemReader() instanceof Skippable) {
					((Skippable) getItemReader()).skip();
				}
				if (getItemWriter() instanceof Skippable) {
					((Skippable) getItemWriter()).skip();
				}
			}
			throw e;
		}
	}

}
