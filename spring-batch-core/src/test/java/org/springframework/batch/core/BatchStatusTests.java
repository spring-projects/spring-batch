/*
 * Copyright 2006-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class BatchStatusTests {

	/**
	 * Test method for {@link org.springframework.batch.core.BatchStatus#toString()}.
	 */
	@Test
	void testToString() {
		assertEquals("ABANDONED", BatchStatus.ABANDONED.toString());
	}

	@Test
	void testMaxStatus() {
		assertEquals(BatchStatus.FAILED, BatchStatus.max(BatchStatus.FAILED, BatchStatus.COMPLETED));
		assertEquals(BatchStatus.FAILED, BatchStatus.max(BatchStatus.COMPLETED, BatchStatus.FAILED));
		assertEquals(BatchStatus.FAILED, BatchStatus.max(BatchStatus.FAILED, BatchStatus.FAILED));
		assertEquals(BatchStatus.STARTED, BatchStatus.max(BatchStatus.STARTED, BatchStatus.STARTING));
		assertEquals(BatchStatus.STARTED, BatchStatus.max(BatchStatus.COMPLETED, BatchStatus.STARTED));
	}

	@Test
	void testUpgradeStatusFinished() {
		assertEquals(BatchStatus.FAILED, BatchStatus.FAILED.upgradeTo(BatchStatus.COMPLETED));
		assertEquals(BatchStatus.FAILED, BatchStatus.COMPLETED.upgradeTo(BatchStatus.FAILED));
	}

	@Test
	void testUpgradeStatusUnfinished() {
		assertEquals(BatchStatus.COMPLETED, BatchStatus.STARTING.upgradeTo(BatchStatus.COMPLETED));
		assertEquals(BatchStatus.COMPLETED, BatchStatus.COMPLETED.upgradeTo(BatchStatus.STARTING));
		assertEquals(BatchStatus.STARTED, BatchStatus.STARTING.upgradeTo(BatchStatus.STARTED));
		assertEquals(BatchStatus.STARTED, BatchStatus.STARTED.upgradeTo(BatchStatus.STARTING));
	}

	@Test
	void testIsRunning() {
		assertFalse(BatchStatus.FAILED.isRunning());
		assertFalse(BatchStatus.COMPLETED.isRunning());
		assertTrue(BatchStatus.STARTED.isRunning());
		assertTrue(BatchStatus.STARTING.isRunning());
		assertTrue(BatchStatus.STOPPING.isRunning());
	}

	@Test
	void testIsUnsuccessful() {
		assertTrue(BatchStatus.FAILED.isUnsuccessful());
		assertFalse(BatchStatus.COMPLETED.isUnsuccessful());
		assertFalse(BatchStatus.STARTED.isUnsuccessful());
		assertFalse(BatchStatus.STARTING.isUnsuccessful());
	}

	@Test
	void testGetStatus() {
		assertEquals(BatchStatus.FAILED, BatchStatus.valueOf(BatchStatus.FAILED.toString()));
	}

	@Test
	void testGetStatusWrongCode() {
		assertThrows(IllegalArgumentException.class, () -> BatchStatus.valueOf("foo"));
	}

	@Test
	void testGetStatusNullCode() {
		assertThrows(NullPointerException.class, () -> BatchStatus.valueOf(null));
	}

	@Test
	void testSerialization() throws Exception {

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
