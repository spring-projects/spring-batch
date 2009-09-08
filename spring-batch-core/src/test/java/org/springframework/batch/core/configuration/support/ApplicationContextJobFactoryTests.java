package org.springframework.batch.core.configuration.support;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

public class ApplicationContextJobFactoryTests {

	private AbstractGroupAwareJobFactory factory = new ApplicationContextJobFactory(
			new StubApplicationContextFactory(), "job");

	@Test
	public void testFactoryContext() throws Exception {
		assertNotNull(factory.createJob());
	}

	private static class StubApplicationContextFactory implements
			ApplicationContextFactory {
		public ConfigurableApplicationContext createApplicationContext() {
			StaticApplicationContext context = new StaticApplicationContext();
			context.registerSingleton("job", JobSupport.class);
			return context ;
		}

	}

}
