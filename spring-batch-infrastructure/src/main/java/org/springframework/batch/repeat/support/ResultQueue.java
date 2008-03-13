/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.batch.repeat.support;


/**
 * Abstraction for queue of {@link ResultHolder} objects. Acts as a BlockingQueue with the ability to count the number
 * of items it expects to ever hold. When clients schedule an item to be added they call {@link #expect()}, and then
 * when the result is collected the queue is notified that it no longer expects another.
 * 
 * @author Dave Syer
 * @author Ben Hale
 */
interface ResultQueue extends RepeatInternalState {

	boolean isEmpty();

	ResultHolder take();

	void expect();

	void put(ResultHolder holder);

	public boolean isExpecting();
}
