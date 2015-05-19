/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.batch.item.support;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

/**
 * 
 * This is a simple ItemStreamReader decorator with a synchronized ItemReader.read() 
 * method - which makes a non-thread-safe ItemReader thread-safe.
 * 
 * However, if reprocessing an item is problematic then using this will make a job not 
 * restartable.  If a restartable job is desired in that case, then further co-ordination 
 * between the read and close methods needs to be implemented.
 * 
 * Here are some links about the motivation behind this class:
 * - http://projects.spring.io/spring-batch/faq.html#threading-reader}
 * - http://stackoverflow.com/a/20002493/2910265}
 * 
 * @author Matthew Ouyang
 * @since 3.0
 *
 * @param <T>
 */
public class SynchronizedItemStreamReader<T> implements ItemStream, ItemReader<T> {

	ItemStreamReader<T> itemStreamReader;

	public void setItemStreamReader(ItemStreamReader<T> itemStreamReader) {
		this.itemStreamReader = itemStreamReader;
	}

	/**
	 * This delegates to the read method of the <code>itemStreamReader</code>
	 */
	public synchronized T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		return this.itemStreamReader.read();
	}

	public void close() {
		this.itemStreamReader.close();
	}

	public void open(ExecutionContext executionContext) {
		this.itemStreamReader.open(executionContext);
	}

	public void update(ExecutionContext executionContext) {
		this.itemStreamReader.update(executionContext);
	}
}
