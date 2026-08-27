/*
 * Copyright 2006-2026 the original author or authors.
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
package org.springframework.batch.integration.async;

import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @param <I> the input object type
 * @param <O> the output object type
 * @deprecated since 6.0.4 in favor of
 * {@link org.springframework.batch.core.step.item.AsyncItemProcessor}
 */
@Deprecated(since = "6.0.4")
public class AsyncItemProcessor<I, O> extends org.springframework.batch.core.step.item.AsyncItemProcessor<I, O> {

	/**
	 * Create a new {@link AsyncItemProcessor} with the delegate {@link ItemProcessor}.
	 * @param delegate the {@link ItemProcessor} to use as a delegate
	 * @since 6.0
	 */
	public AsyncItemProcessor(ItemProcessor<I, O> delegate) {
		super(delegate);
	}

}
