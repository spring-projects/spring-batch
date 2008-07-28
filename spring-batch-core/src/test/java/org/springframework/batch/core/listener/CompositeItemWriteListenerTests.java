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

import static org.easymock.EasyMock.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.ItemWriteListener;

/**
 * @author Lucas Ward
 *
 */
public class CompositeItemWriteListenerTests {

	ItemWriteListener listener;
	CompositeItemWriteListener compositeListener;
	
	@Before
	public void setUp() throws Exception {
	
		listener = createMock(ItemWriteListener.class);
		compositeListener = new CompositeItemWriteListener();
		compositeListener.register(listener);
	}
	
	@Test
	public void testBeforeWrite(){
		Object item = new Object();
		listener.beforeWrite(item);
		replay(listener);
		compositeListener.beforeWrite(item);
		verify(listener);
	}
	
	@Test
	public void testAfterWrite(){
		Object item = new Object();
		listener.afterWrite(item);
		replay(listener);
		compositeListener.afterWrite(item);
		verify(listener);
	}
	
	@Test
	public void testOnWriteError(){
		Object item = new Object();
		Exception ex = new Exception();
		listener.onWriteError(ex, item);
		replay(listener);
		compositeListener.onWriteError(ex, item);
		verify(listener);
	}

	@Test
	public void testSetListners() throws Exception {
		compositeListener.setListeners(new ItemWriteListener[] {listener});
		Object item = new Object();
		listener.beforeWrite(item);
		replay(listener);
		compositeListener.beforeWrite(item);
		verify(listener);
	}

}
