package org.springframework.batch.core.step.handler;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Tests for {@link SimpleSystemProcessExitCodeMapper}.
 */
public class SimpleSystemProcessExitCodeMapperTests {

	private SimpleSystemProcessExitCodeMapper mapper = new SimpleSystemProcessExitCodeMapper();
	
	/**
	 * 0 	-> ExitStatus.FINISHED
	 * else	-> ExitStatus.FAILED
	 */
	@Test
	public void testMapping() {
		assertEquals(ExitStatus.FINISHED, mapper.getExitStatus(0));
		assertEquals(ExitStatus.FAILED, mapper.getExitStatus(1));
		assertEquals(ExitStatus.FAILED, mapper.getExitStatus(-1));
	}
}
