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

import java.util.List;

import junit.framework.TestCase;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
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
		
	//AbstractInputSource should synchronize on first call to read.
	public void testSynchronizationRegistration(){
		
		source.registerSynchronization();
		
		List synchronizations = (List)TransactionSynchronizationManager.getSynchronizations();
		assertEquals(1, synchronizations.size());
	}
	
	public void testCommit(){
		
		source.registerSynchronization();
		commit();
		
		assertTrue(source.commitCalled);
		assertFalse(source.rollbackCalled);
	}
	
	public void testRollback(){
		
		source.registerSynchronization();
		
		rollback();
		
		assertFalse(source.commitCalled);
		assertTrue(source.rollbackCalled);
	}
	
	public void testCommitUnsynchronizedSource(){
		
		commit();
		
		assertFalse(source.commitCalled);
		assertFalse(source.rollbackCalled);
	}
	
	public void testMultipleSynchronizations(){
		
		source.registerSynchronization();
		source.registerSynchronization();
		
		//multiple calls to read should result in only one synchronization
		List synchronizations = (List)TransactionSynchronizationManager.getSynchronizations();
		assertEquals(1, synchronizations.size()); 
	}
	
	public void testUnknownStatus(){
		
		invokeUnknown();
		
		assertFalse(source.commitCalled);
		assertFalse(source.rollbackCalled);
	}
	
	private static class MockIoSource extends AbstractTransactionalIoSource{
		
		private boolean commitCalled = false;
		private boolean rollbackCalled = false;
		
		protected void transactionCommitted() {
			Assert.isTrue(!commitCalled, "Commit aleady called");
			commitCalled = true;
		}

		protected void transactionRolledBack() {
			Assert.isTrue(!rollbackCalled, "Rollback aleady called");
			rollbackCalled = true;
		}		
	}
	
	private void commit() {
		TransactionSynchronizationUtils.invokeAfterCompletion(
				TransactionSynchronizationManager.getSynchronizations(),
				TransactionSynchronization.STATUS_COMMITTED);
	}

	private void rollback() {
		TransactionSynchronizationUtils.invokeAfterCompletion(
				TransactionSynchronizationManager.getSynchronizations(),
				TransactionSynchronization.STATUS_ROLLED_BACK);
	}
	
	private void invokeUnknown() {
		TransactionSynchronizationUtils.invokeAfterCompletion(
				TransactionSynchronizationManager.getSynchronizations(),
				TransactionSynchronization.STATUS_UNKNOWN);
	}
}
