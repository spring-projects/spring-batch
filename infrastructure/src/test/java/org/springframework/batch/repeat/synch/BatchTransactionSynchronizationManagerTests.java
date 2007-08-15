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

package org.springframework.batch.repeat.synch;

import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class BatchTransactionSynchronizationManagerTests extends TestCase {

	private TransactionSynchronization synchronization = new TransactionSynchronizationAdapter() {
	};

	protected void setUp() throws Exception {
		super.setUp();
		BatchTransactionSynchronizationManager.clearSynchronizations();
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
		TransactionSynchronizationManager.initSynchronization();
		RepeatSynchronizationManager.register(new RepeatContextSupport(null));
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		RepeatSynchronizationManager.clear();
	}

	public void testRegisterWhenContextMissing() throws Exception {
		RepeatSynchronizationManager.clear();
		try {
			BatchTransactionSynchronizationManager.registerSynchronization(synchronization);
			List synchronizations = TransactionSynchronizationManager.getSynchronizations();
			assertEquals(1, synchronizations.size());
		}
		catch (IllegalStateException e) {
			fail("Unexpected IllegalStateException");
			// Unexpected -
		}
	}

	public void testRegisterSynchronization() {

		BatchTransactionSynchronizationManager.registerSynchronization(synchronization);

		// There should be only one transaction synchronization object in the
		// list.
		List synchronizations = TransactionSynchronizationManager.getSynchronizations();
		assertEquals(1, synchronizations.size());
		assertSame(synchronizations.get(0), synchronization);

		if (RepeatSynchronizationManager.getContext() != null) {
			assertEquals(1, RepeatSynchronizationManager.getContext().attributeNames().length);
		}
	}

	public void testRegisterSynchronizationWithParentContext() {

		RepeatSynchronizationManager.register(new RepeatContextSupport(RepeatSynchronizationManager.getContext()));

		BatchTransactionSynchronizationManager.registerSynchronization(synchronization);

		// There should be only one transaction synchronization object in the
		// list.
		List synchronizations = TransactionSynchronizationManager.getSynchronizations();
		assertEquals(1, synchronizations.size());
		assertSame(synchronizations.get(0), synchronization);

		assertEquals(0, RepeatSynchronizationManager.getContext().attributeNames().length);
		assertEquals(1, RepeatSynchronizationManager.getContext().getParent().attributeNames().length);
	}

	public void testSynchronizeTwiceWithSameObject() throws Exception {
		BatchTransactionSynchronizationManager.registerSynchronization(synchronization);
		testRegisterSynchronization();
	}

	public void testSynchronizeTwiceWithSameObjectAndNoContext() throws Exception {
		RepeatSynchronizationManager.clear();
		BatchTransactionSynchronizationManager.registerSynchronization(synchronization);
		testRegisterSynchronization();
	}

	public void testReregisterSynchronization() {
		BatchTransactionSynchronizationManager.registerSynchronization(synchronization);
		TransactionSynchronizationManager.clearSynchronization();

		TransactionSynchronizationManager.initSynchronization();
		BatchTransactionSynchronizationManager.resynchronize();

		// There should be only one transaction synchronization object in the
		// list.
		List synchronizations = TransactionSynchronizationManager.getSynchronizations();
		assertEquals(1, synchronizations.size());
		assertSame(synchronizations.get(0), synchronization);

	}

	public void testResynchronizeWithNoSynchronizations() throws Exception {
		BatchTransactionSynchronizationManager.resynchronize();
		List synchronizations = TransactionSynchronizationManager.getSynchronizations();
		assertEquals(0, synchronizations.size());
	}

	public void testSynchronizeWhenNotInTransaction() throws Exception {

		TransactionSynchronizationManager.clearSynchronization();
		BatchTransactionSynchronizationManager.registerSynchronization(synchronization);
		TransactionSynchronizationManager.initSynchronization();

		BatchTransactionSynchronizationManager.resynchronize();

		// There should be only one transaction synchronization object in the
		// list.
		List synchronizations = TransactionSynchronizationManager.getSynchronizations();
		assertEquals(1, synchronizations.size());
		assertSame(synchronizations.get(0), synchronization);
	}

}
