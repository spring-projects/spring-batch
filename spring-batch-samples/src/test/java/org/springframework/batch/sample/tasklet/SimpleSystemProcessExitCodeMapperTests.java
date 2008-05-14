package org.springframework.batch.sample.tasklet;

import junit.framework.TestCase;

import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.sample.tasklet.SimpleSystemProcessExitCodeMapper;

/**
 * Tests for {@link SimpleSystemProcessExitCodeMapper}.
 */
public class SimpleSystemProcessExitCodeMapperTests extends TestCase {

	private SimpleSystemProcessExitCodeMapper mapper = new SimpleSystemProcessExitCodeMapper();
	
	/**
	 * 0 	-> ExitStatus.FINISHED
	 * else	-> ExitStatus.FAILED
	 */
	public void testMapping() {
		assertEquals(ExitStatus.FINISHED, mapper.getExitStatus(0));
		assertEquals(ExitStatus.FAILED, mapper.getExitStatus(1));
		assertEquals(ExitStatus.FAILED, mapper.getExitStatus(-1));
	}
}
