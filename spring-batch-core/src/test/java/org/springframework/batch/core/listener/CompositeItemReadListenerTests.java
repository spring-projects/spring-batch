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

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.ItemReadListener;

/**
 * @author Lucas Ward
 *
 */
public class CompositeItemReadListenerTests {
	
	ItemReadListener<Object> listener;
	CompositeItemReadListener<Object> compositeListener;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		listener = createMock(ItemReadListener.class);
		compositeListener = new CompositeItemReadListener<Object>();
		compositeListener.register(listener);
	}
	
	@Test
	public void testBeforeRead(){
		
		listener.beforeRead();
		replay(listener);
		compositeListener.beforeRead();
		verify(listener);
	}
	
	@Test
	public void testAfterRead(){
		Object item = new Object();
		listener.afterRead(item);
		replay(listener);
		compositeListener.afterRead(item);
		verify(listener);
	}
	
	@Test
	public void testOnReadError(){
		
		Exception ex = new Exception();
		listener.onReadError(ex);
		replay(listener);
		compositeListener.onReadError(ex);
		verify(listener);
	}

	@Test
	public void testSetListeners() throws Exception {
		compositeListener.setListeners(new ArrayList<ItemReadListener<? super Object>>() {
			{
				add(listener);
			}
		});
		listener.beforeRead();
		replay(listener);
		compositeListener.beforeRead();
		verify(listener);
	}
	
}
