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
package org.springframework.batch.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

/**
 * @author Dave Syer
 * 
 */
public class BatchStatusTests {

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.BatchStatus#toString()}.
	 */
	@Test
	public void testToString() {
		assertEquals("FAILED", BatchStatus.FAILED.toString());
	}

	@Test
	public void testMaxStatus() {
		assertEquals(BatchStatus.INCOMPLETE, BatchStatus.max(BatchStatus.INCOMPLETE,BatchStatus.COMPLETED));
		assertEquals(BatchStatus.INCOMPLETE, BatchStatus.max(BatchStatus.COMPLETED, BatchStatus.INCOMPLETE));
		assertEquals(BatchStatus.INCOMPLETE, BatchStatus.max(BatchStatus.INCOMPLETE, BatchStatus.INCOMPLETE));
		assertEquals(BatchStatus.STARTED, BatchStatus.max(BatchStatus.STARTED, BatchStatus.STARTING));
		assertEquals(BatchStatus.STARTED, BatchStatus.max(BatchStatus.COMPLETED, BatchStatus.STARTED));
	}

	@Test
	public void testUpgradeStatusFinished() {
		assertEquals(BatchStatus.INCOMPLETE, BatchStatus.INCOMPLETE.upgradeTo(BatchStatus.COMPLETED));
		assertEquals(BatchStatus.INCOMPLETE, BatchStatus.COMPLETED.upgradeTo(BatchStatus.INCOMPLETE));
	}

	@Test
	public void testUpgradeStatusUnfinished() {
		assertEquals(BatchStatus.COMPLETED, BatchStatus.STARTING.upgradeTo(BatchStatus.COMPLETED));
		assertEquals(BatchStatus.COMPLETED, BatchStatus.COMPLETED.upgradeTo(BatchStatus.STARTING));
		assertEquals(BatchStatus.STARTED, BatchStatus.STARTING.upgradeTo(BatchStatus.STARTED));
		assertEquals(BatchStatus.STARTED, BatchStatus.STARTED.upgradeTo(BatchStatus.STARTING));
	}

	@Test
	public void testIsRunning() {
		assertFalse(BatchStatus.INCOMPLETE.isRunning());
		assertFalse(BatchStatus.COMPLETED.isRunning());
		assertTrue(BatchStatus.STARTED.isRunning());
		assertTrue(BatchStatus.STARTING.isRunning());
	}

	@Test
	public void testIsUnsuccessful() {
		assertTrue(BatchStatus.INCOMPLETE.isUnsuccessful());
		assertFalse(BatchStatus.COMPLETED.isUnsuccessful());
		assertFalse(BatchStatus.STARTED.isUnsuccessful());
		assertFalse(BatchStatus.STARTING.isUnsuccessful());
	}

	@Test
	public void testGetStatus() {
		assertEquals(BatchStatus.INCOMPLETE, BatchStatus.valueOf(BatchStatus.INCOMPLETE.toString()));
	}

	@Test
	public void testGetStatusWrongCode() {
		try {
			BatchStatus.valueOf("foo");
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	@Test(expected=NullPointerException.class)
	public void testGetStatusNullCode() {
		assertNull(BatchStatus.valueOf(null));
	}

	@Test
	public void testSerialization() throws Exception {

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bout);

		out.writeObject(BatchStatus.COMPLETED);
		out.flush();

		ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
		ObjectInputStream in = new ObjectInputStream(bin);

		BatchStatus status = (BatchStatus) in.readObject();
		assertEquals(BatchStatus.COMPLETED, status);
	}
}
