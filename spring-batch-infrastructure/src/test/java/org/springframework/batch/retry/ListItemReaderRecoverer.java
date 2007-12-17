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

package org.springframework.batch.retry;

import java.util.List;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.item.provider.ListItemReader;

public class ListItemReaderRecoverer extends ListItemReader implements ItemReader, ItemRecoverer {

	/**
	 * Delegate to super class constructor.
	 * @param list
	 */
	public ListItemReaderRecoverer(List list) {
		super(list);
	}
	/**
	 * Do nothing. Subclassses should override to implement recovery behaviour.
	 * 
	 * @see org.springframework.batch.item.ItemReader#recover(java.lang.Object,
	 * Throwable)
	 * 
	 * @return false if nothing can be done (the default), or true if the item
	 * can now safely be ignored or committed.
	 */
	public boolean recover(Object item, Throwable cause) {
		return false;
	}

}
