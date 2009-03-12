package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.annotation.BeforeJob;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class DummyAnnotationJobExecutionListener {

	@BeforeJob
	public void execute() {
	}

}
