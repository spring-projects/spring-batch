package org.springframework.batch.core.configuration.xml;


/**
 * @author Dave Syer
 * @since 2.1.2
 */
public class DummyPojoStepExecutionListener extends AbstractTestComponent {

	public void execute() {
		executed = true;
	}

}
