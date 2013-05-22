package org.springframework.batch.core.step.tasklet;

/**
 * 
 * JSR-352 compatible tasklet that provides the 'stop' function.
 * The Spring Batch Tasklet is analogous to 'batchlet' in the JSR
 * terminology
 * 
 * @author Will Schipp
 *
 */
public interface StoppableTasklet extends Tasklet {

	/**
	 * method to signal a long running/looping tasklet to stop
	 * 
	 */
	void stop();
	
}
