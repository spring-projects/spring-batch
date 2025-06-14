/*
 * Copyright 2006-2022 the original author or authors.
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

import java.util.function.Function;

import org.springframework.lang.NonNull;

/**
 * <p>
 * Basic interface for generic output operations. Class implementing this interface will
 * be responsible for serializing objects as necessary. Generally, it is responsibility of
 * implementing class to decide which technology to use for mapping and how it should be
 * configured.
 * </p>
 *
 * <p>
 * The write method is responsible for making sure that any internal buffers are flushed.
 * If a transaction is active it will also usually be necessary to discard the output on a
 * subsequent rollback. The resource to which the writer is sending data should normally
 * be able to handle this itself.
 * </p>
 *
 * @author Dave Syer
 * @author Lucas Ward
 * @author Taeik Lim
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 */
@FunctionalInterface
public interface ItemWriter<T> {

	/**
	 * Process the supplied data element. Will not be called with any null items in normal
	 * operation.
	 * @param chunk of items to be written. Must not be {@code null}.
	 * @throws Exception if there are errors. The framework will catch the exception and
	 * convert or rethrow it as appropriate.
	 */
	void write(@NonNull Chunk<? extends T> chunk) throws Exception;

	/**
	 * Adapts an {@code ItemWriter} accepting items of type {@link U} to one accepting
	 * items of type {@link T} by applying a mapping function to each item before writing.
	 * <p>
	 * The {@code mapping()} item writers are most useful when used in combination with a
	 * {@link org.springframework.batch.item.support.CompositeItemWriter}, where the
	 * mapping function in front of the downstream writer can be a getter of the input
	 * item or a more complex transformation logic.
	 * <p>
	 * This adapter mimics the behavior of
	 * {@link java.util.stream.Collectors#mapping(Function, java.util.stream.Collector)}.
	 * @param <T> the type of the input items
	 * @param <U> type of items accepted by downstream item writer
	 * @param mapper a function to be applied to the input items
	 * @param downstream an item writer which will accept mapped items
	 * @return an item writer which applies the mapping function to the input items and
	 * provides the mapped results to the downstream item writer
	 * @since 6.0
	 */
	static <T, U> ItemWriter<T> mapping(Function<? super T, ? extends U> mapper, ItemWriter<? super U> downstream) {
		return chunk -> downstream.write(new Chunk<>(chunk.getItems().stream().map(mapper).toList()));
	}

}
