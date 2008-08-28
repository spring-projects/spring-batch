package org.springframework.batch.core.step.tasklet;

import org.springframework.batch.repeat.ExitStatus;

/**
 * Simple {@link SystemProcessExitCodeMapper} implementation that performs following mapping:
 * 
 * 0 	-> ExitStatus.FINISHED
 * else	-> ExitStatus.FAILED
 * 
 * @author Robert Kasanicky
 */
public class SimpleSystemProcessExitCodeMapper implements SystemProcessExitCodeMapper {
	public ExitStatus getExitStatus(int exitCode) {
		if (exitCode == 0) {
			return ExitStatus.FINISHED;
		} else {
			return ExitStatus.FAILED;
		}
	}

}
