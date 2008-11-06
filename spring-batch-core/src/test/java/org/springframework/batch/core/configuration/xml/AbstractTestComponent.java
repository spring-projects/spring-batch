package org.springframework.batch.core.configuration.xml;

public class AbstractTestComponent {

	protected boolean executed = false;

	public AbstractTestComponent() {
		super();
	}

	public boolean isExecuted() {
		return executed;
	}

}