/*
 * Copyright 2006-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Interface for item transformation. Given an item as input, this interface provides
 * an extension point which allows for the application of business logic in an item 
 * oriented processing scenario. It should be noted that while it's possible to return
 * a different type than the one provided, it's not strictly necessary. Furthermore, 
 * returning {@code null} indicates that the item should not be continued to be processed.
 *  
 * @author Robert Kasanicky
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * 
 * @param <I> type of input item
 * @param <O> type of output item
 */
public interface ItemProcessor<I, O> {

	/**
	 * Process the provided item, returning a potentially modified or new item for continued
	 * processing.  If the returned result is {@code null}, it is assumed that processing of the item
	 * should not continue.
	 * 
	 * A {@code null} item will never reach this method because the only possible sources are:
	 * <ul>
	 *     <li>an {@link ItemReader} (which indicates no more items)</li>
	 *     <li>a previous {@link ItemProcessor} in a composite processor (which indicates a filtered item)</li>
	 * </ul>
	 * 
	 * @param item to be processed, never {@code null}.
	 * @return potentially modified or new item for continued processing, {@code null} if processing of the
	 *  provided item should not continue.
	 * @throws Exception thrown if exception occurs during processing.
	 */
	@Nullable
	O process(@NonNull I item) throws Exception;
}
