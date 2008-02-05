/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.io.support;

import junit.framework.TestCase;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * @author Lucas Ward
 *
 */
public class AbstractTransactionalIoSourceTests extends TestCase {

	private MockIoSource source;
			
	protected void setUp() throws Exception {
		super.setUp();
		
		source = new MockIoSource();
		if(TransactionSynchronizationManager.isSynchronizationActive()){
			TransactionSynchronizationManager.clearSynchronization();
		}
		TransactionSynchronizationManager.initSynchronization();
	}
		
	public void testCommit(){
		source.mark();
		assertTrue(source.commitCalled);
		assertFalse(source.rollbackCalled);
	}
	
	public void testRollback(){
		source.reset();
		assertFalse(source.commitCalled);
		assertTrue(source.rollbackCalled);
	}
	
	private static class MockIoSource extends AbstractTransactionalIoSource {
		
		private boolean commitCalled = false;
		private boolean rollbackCalled = false;
		
		public void mark() {
			Assert.isTrue(!commitCalled, "Commit aleady called");
			commitCalled = true;
		}

		public void reset() {
			Assert.isTrue(!rollbackCalled, "Rollback aleady called");
			rollbackCalled = true;
		}

		public Object read() throws Exception {
			return null;
		}
	}
	
}
