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
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.springframework.batch.core.ExitStatus;

/**
 * @author Dave Syer
 * 
 */
public class ExitStatusTests {

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.ExitStatus#ExitStatus(boolean, String)}
	 * .
	 */
	@Test
	public void testExitStatusBooleanInt() {
		ExitStatus status = new ExitStatus(true, "10");
		assertTrue(status.isContinuable());
		assertEquals("10", status.getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.ExitStatus#ExitStatus(boolean, String)}
	 * .
	 */
	@Test
	public void testExitStatusConstantsContinuable() {
		ExitStatus status = ExitStatus.EXECUTING;
		assertTrue(status.isContinuable());
		assertEquals("EXECUTING", status.getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.ExitStatus#ExitStatus(boolean, String)}
	 * .
	 */
	@Test
	public void testExitStatusConstantsFinished() {
		ExitStatus status = ExitStatus.FINISHED;
		assertFalse(status.isContinuable());
		assertEquals("COMPLETED", status.getExitCode());
	}

	/**
	 * Test equality of exit statuses.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEqualsWithSameProperties() throws Exception {
		assertEquals(ExitStatus.EXECUTING, new ExitStatus(true, "EXECUTING"));
	}

	@Test
	public void testEqualsSelf() {
		ExitStatus status = new ExitStatus(true, "test");
		assertEquals(status, status);
	}

	@Test
	public void testEquals() {
		assertEquals(new ExitStatus(true, "test"), new ExitStatus(true, "test"));
	}

	/**
	 * Test equality of exit statuses.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEqualsWithNull() throws Exception {
		assertFalse(ExitStatus.EXECUTING.equals(null));
	}

	/**
	 * Test equality of exit statuses.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testHashcode() throws Exception {
		assertEquals(ExitStatus.EXECUTING.toString().hashCode(), ExitStatus.EXECUTING.hashCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.ExitStatus#and(boolean)}.
	 */
	@Test
	public void testAndBoolean() {
		assertTrue(ExitStatus.EXECUTING.and(true).isContinuable());
		assertFalse(ExitStatus.EXECUTING.and(false).isContinuable());
		ExitStatus status = new ExitStatus(false, "CUSTOM_CODE", "CUSTOM_DESCRIPTION");
		assertTrue(status.and(true).getExitCode() == "CUSTOM_CODE");
		assertTrue(status.and(true).getExitDescription() == "CUSTOM_DESCRIPTION");
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.ExitStatus#and(org.springframework.batch.core.ExitStatus)}
	 * .
	 */
	@Test
	public void testAndExitStatusStillContinuable() {
		assertTrue(ExitStatus.EXECUTING.and(ExitStatus.EXECUTING).isContinuable());
		assertFalse(ExitStatus.EXECUTING.and(ExitStatus.FINISHED).isContinuable());
		assertTrue(ExitStatus.EXECUTING.and(ExitStatus.EXECUTING).getExitCode().equals(
				ExitStatus.EXECUTING.getExitCode()));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.ExitStatus#and(org.springframework.batch.core.ExitStatus)}
	 * .
	 */
	@Test
	public void testAndExitStatusWhenFinishedAddedToContinuable() {
		assertEquals(ExitStatus.FINISHED.getExitCode(), ExitStatus.EXECUTING.and(ExitStatus.FINISHED).getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.ExitStatus#and(org.springframework.batch.core.ExitStatus)}
	 * .
	 */
	@Test
	public void testAndExitStatusWhenContinuableAddedToFinished() {
		assertEquals(ExitStatus.FINISHED.getExitCode(), ExitStatus.FINISHED.and(ExitStatus.EXECUTING).getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.ExitStatus#and(org.springframework.batch.core.ExitStatus)}
	 * .
	 */
	@Test
	public void testAndExitStatusWhenCustomContinuableAddedToContinuable() {
		assertEquals("CUSTOM", ExitStatus.EXECUTING.and(ExitStatus.EXECUTING.replaceExitCode("CUSTOM"))
				.getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.ExitStatus#and(org.springframework.batch.core.ExitStatus)}
	 * .
	 */
	@Test
	public void testAndExitStatusFailedPlusFinished() {
		assertEquals("FAILED", ExitStatus.FINISHED.and(ExitStatus.FAILED).getExitCode());
		assertEquals("FAILED", ExitStatus.FAILED.and(ExitStatus.FINISHED).getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.ExitStatus#and(org.springframework.batch.core.ExitStatus)}
	 * .
	 */
	@Test
	public void testAndExitStatusWhenCustomContinuableAddedToFinished() {
		assertEquals(ExitStatus.FINISHED.getExitCode(), ExitStatus.FINISHED.and(
				ExitStatus.EXECUTING.replaceExitCode("CUSTOM")).getExitCode());
	}

	@Test
	public void testAddExitCode() throws Exception {
		ExitStatus status = ExitStatus.EXECUTING.replaceExitCode("FOO");
		assertTrue(ExitStatus.EXECUTING != status);
		assertTrue(status.isContinuable());
		assertEquals("FOO", status.getExitCode());
	}

	@Test
	public void testAddExitCodeToExistingStatus() throws Exception {
		ExitStatus status = ExitStatus.EXECUTING.replaceExitCode("FOO").replaceExitCode("BAR");
		assertTrue(ExitStatus.EXECUTING != status);
		assertTrue(status.isContinuable());
		assertEquals("BAR", status.getExitCode());
	}

	@Test
	public void testAddExitCodeToSameStatus() throws Exception {
		ExitStatus status = ExitStatus.EXECUTING.replaceExitCode(ExitStatus.EXECUTING.getExitCode());
		assertTrue(ExitStatus.EXECUTING != status);
		assertTrue(status.isContinuable());
		assertEquals(ExitStatus.EXECUTING.getExitCode(), status.getExitCode());
	}

	@Test
	public void testAddExitDescription() throws Exception {
		ExitStatus status = ExitStatus.EXECUTING.addExitDescription("Foo");
		assertTrue(ExitStatus.EXECUTING != status);
		assertTrue(status.isContinuable());
		assertEquals("Foo", status.getExitDescription());
	}

	@Test
	public void testAddExitDescriptionToSameStatus() throws Exception {
		ExitStatus status = ExitStatus.EXECUTING.addExitDescription("Foo").addExitDescription("Foo");
		assertTrue(ExitStatus.EXECUTING != status);
		assertTrue(status.isContinuable());
		assertEquals("Foo", status.getExitDescription());
	}

	@Test
	public void testAddEmptyExitDescription() throws Exception {
		ExitStatus status = ExitStatus.EXECUTING.addExitDescription("Foo").addExitDescription(null);
		assertEquals("Foo", status.getExitDescription());
	}

	@Test
	public void testAddExitCodeWithDescription() throws Exception {
		ExitStatus status = new ExitStatus(true, "BAR", "Bar").replaceExitCode("FOO");
		assertEquals("FOO", status.getExitCode());
		assertEquals("Bar", status.getExitDescription());
	}

	@Test
	public void testUnkownIsRunning() throws Exception {
		assertTrue(ExitStatus.UNKNOWN.isRunning());
	}

	@Test
	public void testSerializable() throws Exception {
		ExitStatus status = ExitStatus.EXECUTING.replaceExitCode("FOO");
		byte[] bytes = SerializationUtils.serialize(status);
		Object object = SerializationUtils.deserialize(bytes);
		assertTrue(object instanceof ExitStatus);
		ExitStatus restored = (ExitStatus) object;
		assertTrue(restored.isContinuable());
		assertEquals(status.getExitCode(), restored.getExitCode());
	}
}
