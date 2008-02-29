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
package org.springframework.batch.execution.listener;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.core.domain.ItemWriteListener;

/**
 * @author Lucas Ward
 *
 */
public class CompositeItemWriteListenerTests extends TestCase {

	MockControl listenerControl = MockControl.createControl(ItemWriteListener.class);
	
	ItemWriteListener listener;
	CompositeItemWriteListener compositeListener;
	
	protected void setUp() throws Exception {
		super.setUp();
	
		listener = (ItemWriteListener)listenerControl.getMock();
		compositeListener = new CompositeItemWriteListener();
		compositeListener.register(listener);
	}
	
	public void testBeforeWrite(){
		Object item = new Object();
		listener.beforeWrite(item);
		listenerControl.replay();
		compositeListener.beforeWrite(item);
		listenerControl.verify();
	}
	
	public void testAfterWrite(){
		listener.afterWrite();
		listenerControl.replay();
		compositeListener.afterWrite();
		listenerControl.verify();
	}
	
	public void testOnWriteError(){
		Object item = new Object();
		Exception ex = new Exception();
		listener.onWriteError(ex, item);
		listenerControl.replay();
		compositeListener.onWriteError(ex, item);
		listenerControl.verify();
	}
}
