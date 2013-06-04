package org.springframework.batch.core.step.tasklet;

/**
 * 
 * Stoppable tasklet
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
