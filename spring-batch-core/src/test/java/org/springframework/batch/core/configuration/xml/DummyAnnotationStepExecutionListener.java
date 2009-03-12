package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.annotation.BeforeStep;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class DummyAnnotationStepExecutionListener {

	@BeforeStep
	public void execute() {
	}

}
