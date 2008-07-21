package org.springframework.batch.core.configuration.support;

import org.springframework.context.ConfigurableApplicationContext;

public interface ApplicationContextFactory {

	ConfigurableApplicationContext createApplicationContext();
	
}
