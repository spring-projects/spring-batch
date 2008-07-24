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

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.MarkFailedException;
import org.springframework.batch.item.ResetFailedException;

public class MockItemReader implements ItemReader<String> {

	private final int returnItemCount;

	private int returnedItemCount;
	
	private boolean fail = false;

	public MockItemReader() {
		this(-1);
	}

	public MockItemReader(int returnItemCount) {
		this.returnItemCount = returnItemCount;
	}
	
	public void setFail(boolean fail) {
	    this.fail = fail;
    }

	public void close() {
	}

	public String read() {
		if(fail) {
			fail = false;
			throw new RuntimeException();
		}
		
		if (returnItemCount < 0 || returnedItemCount < returnItemCount) {
			return String.valueOf(returnedItemCount++);
		}
		return null;
	}

	public Object getKey(Object item) {
		return null;
	}

	public void mark() throws MarkFailedException {
	}
	
	public void reset() throws ResetFailedException {
	}

}
