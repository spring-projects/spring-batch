package org.springframework.batch.core.step;

import org.springframework.batch.core.Step;

/**
 * Interface for holders of a {@link Step} as a convenience for callers who need
 * access to the underlying instance.
 * 
 * @author Dave Syer
 * 
 */
public interface StepHolder {

	Step getStep();

}
