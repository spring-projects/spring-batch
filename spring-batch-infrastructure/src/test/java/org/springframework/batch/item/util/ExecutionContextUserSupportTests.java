package org.springframework.batch.item.util;

import org.springframework.batch.item.util.ExecutionContextUserSupport;

import junit.framework.TestCase;

/**
 * Tests for {@link ExecutionContextUserSupport}.
 */
public class ExecutionContextUserSupportTests extends TestCase {

	ExecutionContextUserSupport tested = new ExecutionContextUserSupport();

	/**
	 * Regular usage scenario - prepends the name (supposed to be unique) to
	 * argument.
	 */
	public void testGetKey() {
		tested.setName("uniqueName");
		assertEquals("uniqueName.key", tested.getKey("key"));
	}

	/**
	 * Exception scenario - name must not be empty.
	 */
	public void testGetKeyWithNoNameSet() {
		tested.setName("");
		try {
			tested.getKey("arbitrary string");
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}
}
