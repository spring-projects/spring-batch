/*
 * Copyright 2006-2025 the original author or authors.
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

import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Dave Syer
 * @author Will Schipp
 *
 */
class CompositeItemProcessListenerTests {

	private ItemProcessListener<Object, Object> listener;

	private CompositeItemProcessListener<Object, Object> compositeListener;

	@SuppressWarnings("unchecked")
	@BeforeEach
	void setUp() {
		listener = mock();
		compositeListener = new CompositeItemProcessListener<>();
		compositeListener.register(listener);
	}

	@Test
	void testBeforeRProcess() {
		Object item = new Object();
		listener.beforeProcess(item);
		compositeListener.beforeProcess(item);
	}

	@Test
	void testAfterRead() {
		Object item = new Object();
		Object result = new Object();
		listener.afterProcess(item, result);
		compositeListener.afterProcess(item, result);
	}

	@Test
	void testOnReadError() {
		Object item = new Object();
		Exception ex = new Exception();
		listener.onProcessError(item, ex);
		compositeListener.onProcessError(item, ex);
	}

	@Test
	void testSetListeners() {
		compositeListener
			.setListeners(Collections.<ItemProcessListener<? super Object, ? super Object>>singletonList(listener));
		listener.beforeProcess(null);
		compositeListener.beforeProcess(null);
	}

}
