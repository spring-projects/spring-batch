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

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.MarkFailedException;
import org.springframework.batch.item.ResetFailedException;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Strategy for processing in a step. Bears a resemblance to {@link ItemReader}
 * and {@link ItemWriter} because part of the contract of the processor is that
 * it should delegate calls to those interfaces.
 * 
 * @author Dave Syer
 * 
 */
public interface StepHandler {

	/**
	 * Given the current context in the form of a step contribution, do whatever
	 * is necessary to process this unit inside a chunk. Implementations obtain
	 * the item and return {@link ExitStatus#FINISHED} if it is null. If it is
	 * not null process the item and return {@link ExitStatus#CONTINUABLE}. On
	 * failure throws an exception.
	 * 
	 * @param contribution the current step context
	 * @return an {@link ExitStatus} indicating whether processing is
	 * continuable.
	 */
	ExitStatus handle(StepContribution contribution) throws Exception;

	/**
	 * Implementations should delegate to an {@link ItemReader}.
	 * 
	 * @see org.springframework.batch.item.ItemReader#mark()
	 */
	void mark() throws MarkFailedException;

	/**
	 * Implementations should delegate to an {@link ItemReader}.
	 * 
	 * @see org.springframework.batch.item.ItemReader#reset()
	 */
	void reset() throws ResetFailedException;

	/**
	 * Implementations should delegate to an {@link ItemWriter}.
	 * 
	 * @see org.springframework.batch.item.ItemWriter#flush()
	 */
	public void flush() throws FlushFailedException;

	/**
	 * Implementations should delegate to an {@link ItemWriter}.
	 * 
	 * @see org.springframework.batch.item.ItemWriter#clear()
	 */
	public void clear() throws ClearFailedException;

}
