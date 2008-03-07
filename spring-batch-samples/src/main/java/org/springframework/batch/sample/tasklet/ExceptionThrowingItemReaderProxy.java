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

package org.springframework.batch.sample.tasklet;


import org.springframework.batch.core.InfrastructureException;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.reader.DelegatingItemReader;

/**
 * Hacked {@link ItemReader} that throws exception on a given record number
 * (useful for testing restart).
 * 
 * @author Robert Kasanicky
 * @author Lucas Ward
 *
 */
public class ExceptionThrowingItemReaderProxy extends DelegatingItemReader {

	private int counter = 0;
	private int throwExceptionOnRecordNumber = 4;
	
	/**
	 * @param throwExceptionOnRecordNumber The number of record on which exception should be thrown
	 */
	public void setThrowExceptionOnRecordNumber(int throwExceptionOnRecordNumber) {
		this.throwExceptionOnRecordNumber = throwExceptionOnRecordNumber;
	}
	
	public int getThrowExceptionOnRecordNumber() {
		return throwExceptionOnRecordNumber;
	}

	public Object read() throws Exception {
		
		counter++;
		if (counter == throwExceptionOnRecordNumber) {
			throw new InfrastructureException("Planned failure on count="+counter);
		}
		
		return super.read();
	}

}
