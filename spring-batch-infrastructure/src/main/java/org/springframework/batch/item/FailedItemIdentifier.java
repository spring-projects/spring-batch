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

package org.springframework.batch.item;

/**
 * Mixin interface for {@link ItemReader} implementations if they can
 * distinguish a new item from one that has been processed before and failed,
 * e.g. by examining a message flag.
 * 
 * @author Dave Syer
 * 
 */
public interface FailedItemIdentifier {

	/**
	 * Inspect the item and determine if it has previously failed processing.
	 * The safest choice when the answer is indeterminate is 'true'.
	 * 
	 * @param item the current item.
	 * @return true if the item has been seen before and is known to have failed
	 * processing.
	 */
	boolean hasFailed(Object item);

}
