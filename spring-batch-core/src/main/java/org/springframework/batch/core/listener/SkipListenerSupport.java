/*
 * Copyright 2006-2021 the original author or authors.
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
package org.springframework.batch.core.listener;

import org.springframework.batch.core.SkipListener;

/**
 * Basic no-op implementations of all {@link SkipListener} implementations.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @deprecated as of v5.0 in favor of the default methods in {@link SkipListener}.
 *
 */
@Deprecated
public class SkipListenerSupport<T, S> implements SkipListener<T, S> {

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.SkipListener#onSkipInRead(java.lang.Throwable)
	 */
	@Override
	public void onSkipInRead(Throwable t) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.SkipListener#onSkipInWrite(java.lang.Object,
	 * java.lang.Throwable)
	 */
	@Override
	public void onSkipInWrite(S item, Throwable t) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.SkipListener#onSkipInProcess(java.lang.Object,
	 * java.lang.Throwable)
	 */
	@Override
	public void onSkipInProcess(T item, Throwable t) {
	}

}
