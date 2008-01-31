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

package org.springframework.batch.item.reader;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.support.AbstractMethodInvokingDelegator;

/**
 * Invokes a custom method which provides an item.
 * 
 * @author Robert Kasanicky
 */
public class ItemReaderAdapter extends AbstractMethodInvokingDelegator implements ItemReader {
	
	/**
	 * @return return value of the target method.
	 */
    public Object read() throws Exception {
		return invokeDelegateMethod();
    }

	/**
	 * Do nothing.
	 * 
	 * @see org.springframework.batch.item.ItemReader#close()
	 */
	public void close() throws Exception {
		
	}

}

