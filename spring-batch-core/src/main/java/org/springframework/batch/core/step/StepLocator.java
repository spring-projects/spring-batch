package org.springframework.batch.core.step;

import java.util.Collection;

import org.springframework.batch.core.Step;

/**
 * Interface for locating a {@link Step} instance by name.
 * 
 * @author Dave Syer
 *
 */
public interface StepLocator {
	
	Collection<String> getStepNames();
	
	Step getStep(String stepName) throws NoSuchStepException;

}
