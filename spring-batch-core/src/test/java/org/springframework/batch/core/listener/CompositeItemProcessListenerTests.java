/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.core.listener;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.ItemProcessListener;

/**
 * @author Dave Syer
 * 
 */
public class CompositeItemProcessListenerTests {

	private ItemProcessListener<Object, Object> listener;

	private CompositeItemProcessListener<Object, Object> compositeListener;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		listener = createMock(ItemProcessListener.class);
		compositeListener = new CompositeItemProcessListener<Object, Object>();
		compositeListener.register(listener);
	}

	@Test
	public void testBeforeRProcess() {
		Object item = new Object();
		listener.beforeProcess(item);
		replay(listener);
		compositeListener.beforeProcess(item);
		verify(listener);
	}

	@Test
	public void testAfterRead() {
		Object item = new Object();
		Object result = new Object();
		listener.afterProcess(item, result);
		replay(listener);
		compositeListener.afterProcess(item, result);
		verify(listener);
	}

	@Test
	public void testOnReadError() {
		Object item = new Object();
		Exception ex = new Exception();
		listener.onProcessError(item, ex);
		replay(listener);
		compositeListener.onProcessError(item, ex);
		verify(listener);
	}

	@Test
	public void testSetListeners() throws Exception {
		compositeListener.setListeners(Collections
				.<ItemProcessListener<? super Object, ? super Object>> singletonList(listener));
		listener.beforeProcess(null);
		replay(listener);
		compositeListener.beforeProcess(null);
		verify(listener);
	}

}
