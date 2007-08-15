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
 * @author Dave Syer
 * 
 */
public interface ItemProcessor {

	/**
	 * Process the supplied data element. Will be called multiple times during a
	 * larger batch operation. Will not be called with null data in normal
	 * operation.
	 * 
	 * @throws Exception if there are errors. If the processor is used inside a
	 * retry or a batch the framework will catch the exception and convert or
	 * rethrow it as appropriate.
	 */
	void process(Object data) throws Exception;

}
