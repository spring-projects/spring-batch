/*
 * Copyright 2006-2010 the original author or authors.
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
 * <p>
 * A specialisation of {@link ItemReader} that allows the user to look ahead
 * into the stream of items. This is useful, for instance, when reading flat
 * file data that contains record separator lines which are actually part of the
 * next record.
 * </p>
 * 
 * <p>
 * The detailed contract for {@link #peek()} has to be defined by the
 * implementation because there is no general way to define it in a concurrent
 * environment. The definition of "the next read()" operation is tenuous if
 * multiple clients are reading concurrently, and the ability to peek implies
 * that some state is likely to be stored, so implementations of
 * {@link PeekableItemReader} may well be restricted to single threaded use.
 * </p>
 * 
 * @author Dave Syer
 * 
 */
public interface PeekableItemReader<T> extends ItemReader<T> {

	/**
	 * Get the next item that would be returned by {@link #read()}, without
	 * affecting the result of {@link #read()}.
	 * 
	 * @return the next item
	 * @throws Exception if there is a problem
	 */
	T peek() throws Exception, UnexpectedInputException, ParseException;

}
