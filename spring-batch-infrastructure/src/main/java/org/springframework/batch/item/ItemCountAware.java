/*
 * Copyright 20013 the original author or authors.
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
package org.springframework.batch.item;

import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;

/**
 * Marker interface indicating that an item should have the item count set on it. Typically used within
 * an {@link AbstractItemCountingItemStreamItemReader}.
 *
 * @author Jimmy Praet
 */
public interface ItemCountAware {

	/**
	 * Setter for the injection of the current item count.
	 * 
	 * @param count the number of items that have been processed in this execution.
	 */
	void setItemCount(int count);
}
