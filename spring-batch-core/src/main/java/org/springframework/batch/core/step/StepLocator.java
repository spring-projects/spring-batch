package org.springframework.batch.core.step;

import org.springframework.batch.core.Step;

/**
 * Interface for locating a {@link Step} instance by name.
 * 
 * @author Dave Syer
 *
 */
public interface StepLocator {
	
	Step getStep(String stepName);

}
