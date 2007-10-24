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

import org.apache.commons.lang.SerializationUtils;

import junit.framework.TestCase;

/**
 * @author Dave Syer
 *
 */
public class ExitStatusTests extends TestCase {

	/**
	 * Test method for {@link org.springframework.batch.repeat.ExitStatus#ExitStatus(boolean, int)}.
	 */
	public void testExitStatusBooleanInt() {
		ExitStatus status = new ExitStatus(true, "10");
		assertTrue(status.isContinuable());
		assertEquals("10", status.getExitCode());
	}

	/**
	 * Test method for {@link org.springframework.batch.repeat.ExitStatus#ExitStatus(boolean, int)}.
	 */
	public void testExitStatusConstantsContinuable() {
		ExitStatus status = ExitStatus.CONTINUABLE;
		assertTrue(status.isContinuable());
		assertEquals("CONTINUABLE", status.getExitCode());
	}

	/**
	 * Test method for {@link org.springframework.batch.repeat.ExitStatus#ExitStatus(boolean, int)}.
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

	/**
	 * Test equality of exit statuses.
	 * 
	 * @throws Exception
	 */
	public void testEqualsWithNull() throws Exception {
		assertFalse(ExitStatus.CONTINUABLE.equals(null));
	}

	/**
	 * Test method for {@link org.springframework.batch.repeat.ExitStatus#and(boolean)}.
	 */
	public void testAndBoolean() {
		assertTrue(ExitStatus.CONTINUABLE.and(true).isContinuable());
		assertFalse(ExitStatus.CONTINUABLE.and(false).isContinuable());
	}

	/**
	 * Test method for {@link org.springframework.batch.repeat.ExitStatus#and(org.springframework.batch.repeat.ExitStatus)}.
	 */
	public void testAndExitStatus() {
		assertTrue(ExitStatus.CONTINUABLE.and(ExitStatus.CONTINUABLE.isContinuable()).isContinuable());
		assertFalse(ExitStatus.CONTINUABLE.and(ExitStatus.FINISHED.isContinuable()).isContinuable());
		assertTrue(ExitStatus.FINISHED.and(ExitStatus.CONTINUABLE.isContinuable()).getExitCode() 
				== ExitStatus.FINISHED.getExitCode());
		ExitStatus status = new ExitStatus(false, "CUSTOM_CODE", "CUSTOM_DESCRIPTION");
		assertTrue(status.and(true).getExitCode() == "CUSTOM_CODE");
		assertTrue(status.and(true).getExitDescription() == "CUSTOM_DESCRIPTION");
	}

	public void testAddExitCode() throws Exception {
		ExitStatus status = ExitStatus.CONTINUABLE.addExitCode("FOO");
		assertTrue(ExitStatus.CONTINUABLE!=status);
		assertTrue(status.isContinuable());
		assertEquals("FOO", status.getExitCode());
	}

	public void testAddExitCodeWithDescription() throws Exception {
		ExitStatus status = new ExitStatus(true, "BAR", "Bar").addExitCode("FOO");
		assertEquals("FOO", status.getExitCode());
		assertEquals("Bar", status.getExitDescription());
	}
	
	public void testRunningIsRunning() throws Exception {
		assertTrue(ExitStatus.RUNNING.isRunning());
		assertTrue(new ExitStatus(true, "RUNNING").isRunning());
	}
	
	public void testUnkownIsRunning() throws Exception {
		assertTrue(ExitStatus.UNKNOWN.isRunning());
	}

	public void testSerializable() throws Exception {
		ExitStatus status = ExitStatus.CONTINUABLE.addExitCode("FOO");
		byte[] bytes = SerializationUtils.serialize(status);
		Object object = SerializationUtils.deserialize(bytes);
		assertTrue(object instanceof ExitStatus);
		ExitStatus restored = (ExitStatus) object;
		assertTrue(restored.isContinuable());
		assertEquals("FOO", restored.getExitCode());
	}
}
