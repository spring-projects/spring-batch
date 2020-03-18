/*
 * Copyright 2006-2019 the original author or authors.
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

package org.springframework.batch.item.adapter;

import org.springframework.batch.item.ItemReader;
import org.springframework.lang.Nullable;

/**
 * Invokes a custom method on a delegate plain old Java object which itself
 * provides an item.
 *
 * @author Robert Kasanicky
 */
public class ItemReaderAdapter<T> extends AbstractMethodInvokingDelegator<T> implements ItemReader<T> {

	/**
	 * @return return value of the target method.
	 */
	@Nullable
	@Override
	public T read() throws Exception {
		return invokeDelegateMethod();
	}

}
