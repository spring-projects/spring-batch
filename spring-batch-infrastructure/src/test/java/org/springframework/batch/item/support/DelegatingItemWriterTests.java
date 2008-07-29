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
package org.springframework.batch.item.support;

import junit.framework.TestCase;

import static org.easymock.EasyMock.*;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.DelegatingItemWriter;

/**
 * @author Lucas Ward
 * 
 */
public class DelegatingItemWriterTests extends TestCase {

	@SuppressWarnings("unchecked")
	ItemWriter<Object> itemWriter = createMock(ItemWriter.class);

	DelegatingItemWriter<Object, Object> delegatingWriter;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */

	protected void setUp() throws Exception {
		super.setUp();

		delegatingWriter = new DelegatingItemWriter<Object, Object>();
		delegatingWriter.setDelegate(itemWriter);
	}

	public void testFlush() throws Exception {
		itemWriter.flush();
		expectLastCall().once();
		replay(itemWriter);
		delegatingWriter.flush();
		verify(itemWriter);
	}

	public void testClear() throws Exception {
		itemWriter.clear();
		expectLastCall().once();
		replay(itemWriter);
		delegatingWriter.clear();
		verify(itemWriter);
	}

	public void testCreation() throws Exception {
		try {
			delegatingWriter.setDelegate(null);
			delegatingWriter.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testWrite() throws Exception {

		ProcessingWriter<Object, Object> writer = new ProcessingWriter<Object, Object>();
		writer.setDelegate(itemWriter);
		Object item = new Object();
		itemWriter.write(item);
		expectLastCall().once();
		replay(itemWriter);
		writer.write(item);
		verify(itemWriter);
		assertTrue(writer.isDoProcessCalled());
	}

	private class ProcessingWriter<I, O> extends DelegatingItemWriter<I, O> {

		boolean doProcessCalled = false;

		protected O doProcess(I item) throws Exception {
			doProcessCalled = true;
			return super.doProcess(item);
		}

		public boolean isDoProcessCalled() {
			return doProcessCalled;
		}
	}

}
