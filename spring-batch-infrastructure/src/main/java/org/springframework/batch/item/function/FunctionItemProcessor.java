/*
 * Copyright 2017-2019 the original author or authors.
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
package org.springframework.batch.item.function;

import java.util.function.Function;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An {@link ItemProcessor} implementation that delegates to a {@link Function}
 *
 * @author Michael Minella
 * @since 4.0
 */
public class FunctionItemProcessor<I, O> implements ItemProcessor<I, O>{

	private final Function<I, O> function;

	/**
	 * @param function the delegate.  Must not be null
	 */
	public FunctionItemProcessor(Function<I, O> function) {
		Assert.notNull(function, "A function is required");
		this.function = function;
	}

	@Nullable
	@Override
	public O process(I item) throws Exception {
		return this.function.apply(item);
	}
}
