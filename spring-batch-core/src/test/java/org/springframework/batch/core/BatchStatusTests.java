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
		assertEquals("ABANDONED", BatchStatus.ABANDONED.toString());
	}

	@Test
	public void testMaxStatus() {
		assertEquals(BatchStatus.FAILED, BatchStatus.max(BatchStatus.FAILED,BatchStatus.COMPLETED));
		assertEquals(BatchStatus.FAILED, BatchStatus.max(BatchStatus.COMPLETED, BatchStatus.FAILED));
		assertEquals(BatchStatus.FAILED, BatchStatus.max(BatchStatus.FAILED, BatchStatus.FAILED));
		assertEquals(BatchStatus.STARTED, BatchStatus.max(BatchStatus.STARTED, BatchStatus.STARTING));
		assertEquals(BatchStatus.STARTED, BatchStatus.max(BatchStatus.COMPLETED, BatchStatus.STARTED));
	}

	@Test
	public void testUpgradeStatusFinished() {
		assertEquals(BatchStatus.FAILED, BatchStatus.FAILED.upgradeTo(BatchStatus.COMPLETED));
		assertEquals(BatchStatus.FAILED, BatchStatus.COMPLETED.upgradeTo(BatchStatus.FAILED));
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
		assertFalse(BatchStatus.FAILED.isRunning());
		assertFalse(BatchStatus.COMPLETED.isRunning());
		assertTrue(BatchStatus.STARTED.isRunning());
		assertTrue(BatchStatus.STARTING.isRunning());
	}

	@Test
	public void testIsUnsuccessful() {
		assertTrue(BatchStatus.FAILED.isUnsuccessful());
		assertFalse(BatchStatus.COMPLETED.isUnsuccessful());
		assertFalse(BatchStatus.STARTED.isUnsuccessful());
		assertFalse(BatchStatus.STARTING.isUnsuccessful());
	}

	@Test
	public void testGetStatus() {
		assertEquals(BatchStatus.FAILED, BatchStatus.valueOf(BatchStatus.FAILED.toString()));
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
