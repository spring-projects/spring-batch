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
package org.springframework.batch.repeat;

import junit.framework.TestCase;

import org.apache.commons.lang.SerializationUtils;

/**
 * @author Dave Syer
 * 
 */
public class ExitStatusTests extends TestCase {

	/**
	 * Test method for
	 * {@link org.springframework.batch.repeat.ExitStatus#ExitStatus(boolean, String)}.
	 */
	public void testExitStatusBooleanInt() {
		ExitStatus status = new ExitStatus(true, "10");
		assertTrue(status.isContinuable());
		assertEquals("10", status.getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.repeat.ExitStatus#ExitStatus(boolean, String)}.
	 */
	public void testExitStatusConstantsContinuable() {
		ExitStatus status = ExitStatus.CONTINUABLE;
		assertTrue(status.isContinuable());
		assertEquals("CONTINUABLE", status.getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.repeat.ExitStatus#ExitStatus(boolean, String)}.
	 */
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
	public void testEqualsWithSameProperties() throws Exception {
		assertEquals(ExitStatus.CONTINUABLE, new ExitStatus(true, "CONTINUABLE"));
	}

	public void testEqualsSelf() {
		ExitStatus status = new ExitStatus(true, "test");
		assertEquals(status, status);
	}

	public void testEquals() {
		assertEquals(new ExitStatus(true, "test"), new ExitStatus(true, "test"));
	}

	/**
	 * Test equality of exit statuses.
	 * 
	 * @throws Exception
	 */
	public void testEqualsWithNull() throws Exception {
		assertFalse(ExitStatus.CONTINUABLE.equals(null));
	}

	/**
	 * Test equality of exit statuses.
	 * 
	 * @throws Exception
	 */
	public void testHashcode() throws Exception {
		assertEquals(ExitStatus.CONTINUABLE.toString().hashCode(), ExitStatus.CONTINUABLE.hashCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.repeat.ExitStatus#and(boolean)}.
	 */
	public void testAndBoolean() {
		assertTrue(ExitStatus.CONTINUABLE.and(true).isContinuable());
		assertFalse(ExitStatus.CONTINUABLE.and(false).isContinuable());
		ExitStatus status = new ExitStatus(false, "CUSTOM_CODE", "CUSTOM_DESCRIPTION");
		assertTrue(status.and(true).getExitCode() == "CUSTOM_CODE");
		assertTrue(status.and(true).getExitDescription() == "CUSTOM_DESCRIPTION");
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.repeat.ExitStatus#and(org.springframework.batch.repeat.ExitStatus)}.
	 */
	public void testAndExitStatusStillContinuable() {
		assertTrue(ExitStatus.CONTINUABLE.and(ExitStatus.CONTINUABLE).isContinuable());
		assertFalse(ExitStatus.CONTINUABLE.and(ExitStatus.FINISHED).isContinuable());
		assertTrue(ExitStatus.CONTINUABLE.and(ExitStatus.CONTINUABLE).getExitCode().equals(
				ExitStatus.CONTINUABLE.getExitCode()));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.repeat.ExitStatus#and(org.springframework.batch.repeat.ExitStatus)}.
	 */
	public void testAndExitStatusWhenFinishedAddedToContinuable() {
		assertEquals(ExitStatus.FINISHED.getExitCode(), ExitStatus.CONTINUABLE.and(ExitStatus.FINISHED).getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.repeat.ExitStatus#and(org.springframework.batch.repeat.ExitStatus)}.
	 */
	public void testAndExitStatusWhenContinuableAddedToFinished() {
		assertEquals(ExitStatus.FINISHED.getExitCode(), ExitStatus.FINISHED.and(ExitStatus.CONTINUABLE).getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.repeat.ExitStatus#and(org.springframework.batch.repeat.ExitStatus)}.
	 */
	public void testAndExitStatusWhenCustomContinuableAddedToContinuable() {
		assertEquals("CUSTOM", ExitStatus.CONTINUABLE.and(ExitStatus.CONTINUABLE.replaceExitCode("CUSTOM"))
				.getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.repeat.ExitStatus#and(org.springframework.batch.repeat.ExitStatus)}.
	 */
	public void testAndExitStatusWhenCustomContinuableAddedToFinished() {
		assertEquals(ExitStatus.FINISHED.getExitCode(), ExitStatus.FINISHED.and(
				ExitStatus.CONTINUABLE.replaceExitCode("CUSTOM")).getExitCode());
	}

	public void testAddExitCode() throws Exception {
		ExitStatus status = ExitStatus.CONTINUABLE.replaceExitCode("FOO");
		assertTrue(ExitStatus.CONTINUABLE != status);
		assertTrue(status.isContinuable());
		assertEquals("FOO", status.getExitCode());
	}

	public void testAddExitCodeToExistingStatus() throws Exception {
		ExitStatus status = ExitStatus.CONTINUABLE.replaceExitCode("FOO").replaceExitCode("BAR");
		assertTrue(ExitStatus.CONTINUABLE != status);
		assertTrue(status.isContinuable());
		assertEquals("BAR", status.getExitCode());
	}

	public void testAddExitCodeToSameStatus() throws Exception {
		ExitStatus status = ExitStatus.CONTINUABLE.replaceExitCode(ExitStatus.CONTINUABLE.getExitCode());
		assertTrue(ExitStatus.CONTINUABLE != status);
		assertTrue(status.isContinuable());
		assertEquals(ExitStatus.CONTINUABLE.getExitCode(), status.getExitCode());
	}

	public void testAddExitDescription() throws Exception {
		ExitStatus status = ExitStatus.CONTINUABLE.addExitDescription("Foo");
		assertTrue(ExitStatus.CONTINUABLE != status);
		assertTrue(status.isContinuable());
		assertEquals("Foo", status.getExitDescription());
	}

	public void testAddExitDescriptionToSameStatus() throws Exception {
		ExitStatus status = ExitStatus.CONTINUABLE.addExitDescription("Foo").addExitDescription("Foo");
		assertTrue(ExitStatus.CONTINUABLE != status);
		assertTrue(status.isContinuable());
		assertEquals("Foo", status.getExitDescription());
	}

	public void testAddEmptyExitDescription() throws Exception {
		ExitStatus status = ExitStatus.CONTINUABLE.addExitDescription("Foo").addExitDescription(null);
		assertEquals("Foo", status.getExitDescription());
	}

	public void testAddExitCodeWithDescription() throws Exception {
		ExitStatus status = new ExitStatus(true, "BAR", "Bar").replaceExitCode("FOO");
		assertEquals("FOO", status.getExitCode());
		assertEquals("Bar", status.getExitDescription());
	}

	public void testUnkownIsRunning() throws Exception {
		assertTrue(ExitStatus.UNKNOWN.isRunning());
	}

	public void testSerializable() throws Exception {
		ExitStatus status = ExitStatus.CONTINUABLE.replaceExitCode("FOO");
		byte[] bytes = SerializationUtils.serialize(status);
		Object object = SerializationUtils.deserialize(bytes);
		assertTrue(object instanceof ExitStatus);
		ExitStatus restored = (ExitStatus) object;
		assertTrue(restored.isContinuable());
		assertEquals(status.getExitCode(), restored.getExitCode());
	}
}
