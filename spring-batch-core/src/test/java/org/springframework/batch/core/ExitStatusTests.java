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

import org.junit.Test;
import org.springframework.batch.support.SerializationUtils;

/**
 * @author Dave Syer
 * 
 */
public class ExitStatusTests {

	@Test
	public void testExitStatusNullDescription() {
		ExitStatus status = new ExitStatus("10", null);
		assertEquals("", status.getExitDescription());
	}

	@Test
	public void testExitStatusBooleanInt() {
		ExitStatus status = new ExitStatus("10");
		assertEquals("10", status.getExitCode());
	}

	@Test
	public void testExitStatusConstantsContinuable() {
		ExitStatus status = ExitStatus.EXECUTING;
		assertEquals("EXECUTING", status.getExitCode());
	}

	@Test
	public void testExitStatusConstantsFinished() {
		ExitStatus status = ExitStatus.COMPLETED;
		assertEquals("COMPLETED", status.getExitCode());
	}

	/**
	 * Test equality of exit statuses.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEqualsWithSameProperties() throws Exception {
		assertEquals(ExitStatus.EXECUTING, new ExitStatus("EXECUTING"));
	}

	@Test
	public void testEqualsSelf() {
		ExitStatus status = new ExitStatus("test");
		assertEquals(status, status);
	}

	@Test
	public void testEquals() {
		assertEquals(new ExitStatus("test"), new ExitStatus("test"));
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
	 * {@link org.springframework.batch.core.ExitStatus#and(org.springframework.batch.core.ExitStatus)}
	 * .
	 */
	@Test
	public void testAndExitStatusStillExecutable() {
		assertEquals(ExitStatus.EXECUTING.getExitCode(), ExitStatus.EXECUTING.and(ExitStatus.EXECUTING).getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.ExitStatus#and(org.springframework.batch.core.ExitStatus)}
	 * .
	 */
	@Test
	public void testAndExitStatusWhenFinishedAddedToContinuable() {
		assertEquals(ExitStatus.COMPLETED.getExitCode(), ExitStatus.EXECUTING.and(ExitStatus.COMPLETED).getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.ExitStatus#and(org.springframework.batch.core.ExitStatus)}
	 * .
	 */
	@Test
	public void testAndExitStatusWhenContinuableAddedToFinished() {
		assertEquals(ExitStatus.COMPLETED.getExitCode(), ExitStatus.COMPLETED.and(ExitStatus.EXECUTING).getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.ExitStatus#and(org.springframework.batch.core.ExitStatus)}
	 * .
	 */
	@Test
	public void testAndExitStatusWhenCustomContinuableAddedToContinuable() {
		assertEquals("CUSTOM", ExitStatus.EXECUTING.and(ExitStatus.EXECUTING.replaceExitCode("CUSTOM")).getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.ExitStatus#and(org.springframework.batch.core.ExitStatus)}
	 * .
	 */
	@Test
	public void testAndExitStatusWhenCustomCompletedAddedToCompleted() {
		assertEquals("COMPLETED_CUSTOM", ExitStatus.COMPLETED.and(
				ExitStatus.EXECUTING.replaceExitCode("COMPLETED_CUSTOM")).getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.ExitStatus#and(org.springframework.batch.core.ExitStatus)}
	 * .
	 */
	@Test
	public void testAndExitStatusFailedPlusFinished() {
		assertEquals("FAILED", ExitStatus.COMPLETED.and(ExitStatus.FAILED).getExitCode());
		assertEquals("FAILED", ExitStatus.FAILED.and(ExitStatus.COMPLETED).getExitCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.ExitStatus#and(org.springframework.batch.core.ExitStatus)}
	 * .
	 */
	@Test
	public void testAndExitStatusWhenCustomContinuableAddedToFinished() {
		assertEquals("CUSTOM", ExitStatus.COMPLETED.and(ExitStatus.EXECUTING.replaceExitCode("CUSTOM")).getExitCode());
	}

	@Test
	public void testAddExitCode() throws Exception {
		ExitStatus status = ExitStatus.EXECUTING.replaceExitCode("FOO");
		assertTrue(ExitStatus.EXECUTING != status);
		assertEquals("FOO", status.getExitCode());
	}

	@Test
	public void testAddExitCodeToExistingStatus() throws Exception {
		ExitStatus status = ExitStatus.EXECUTING.replaceExitCode("FOO").replaceExitCode("BAR");
		assertTrue(ExitStatus.EXECUTING != status);
		assertEquals("BAR", status.getExitCode());
	}

	@Test
	public void testAddExitCodeToSameStatus() throws Exception {
		ExitStatus status = ExitStatus.EXECUTING.replaceExitCode(ExitStatus.EXECUTING.getExitCode());
		assertTrue(ExitStatus.EXECUTING != status);
		assertEquals(ExitStatus.EXECUTING.getExitCode(), status.getExitCode());
	}

	@Test
	public void testAddExitDescription() throws Exception {
		ExitStatus status = ExitStatus.EXECUTING.addExitDescription("Foo");
		assertTrue(ExitStatus.EXECUTING != status);
		assertEquals("Foo", status.getExitDescription());
	}

	@Test
	public void testAddExitDescriptionWIthStacktrace() throws Exception {
		ExitStatus status = ExitStatus.EXECUTING.addExitDescription(new RuntimeException("Foo"));
		assertTrue(ExitStatus.EXECUTING != status);
		String description = status.getExitDescription();
		assertTrue("Wrong description: "+description, description.contains("Foo"));
		assertTrue("Wrong description: "+description, description.contains("RuntimeException"));
	}

	@Test
	public void testAddExitDescriptionToSameStatus() throws Exception {
		ExitStatus status = ExitStatus.EXECUTING.addExitDescription("Foo").addExitDescription("Foo");
		assertTrue(ExitStatus.EXECUTING != status);
		assertEquals("Foo", status.getExitDescription());
	}

	@Test
	public void testAddEmptyExitDescription() throws Exception {
		ExitStatus status = ExitStatus.EXECUTING.addExitDescription("Foo").addExitDescription((String)null);
		assertEquals("Foo", status.getExitDescription());
	}

	@Test
	public void testAddExitCodeWithDescription() throws Exception {
		ExitStatus status = new ExitStatus("BAR", "Bar").replaceExitCode("FOO");
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
		assertEquals(status.getExitCode(), restored.getExitCode());
	}
}
