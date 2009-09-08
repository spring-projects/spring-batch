package org.springframework.batch.core.configuration.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

public class ApplicationContextJobFactoryTests {

	@Test
	public void testFactoryContext() throws Exception {
		ApplicationContextJobFactory factory = new ApplicationContextJobFactory("job",
				new StubApplicationContextFactory());
		assertNotNull(factory.createJob());
	}

	@Test
	public void testGroupName() throws Exception {
		ApplicationContextJobFactory factory = new ApplicationContextJobFactory("jobs", "job",
				new StubApplicationContextFactory());
		assertEquals("jobs.job", factory.getJobName());
	}

	private static class StubApplicationContextFactory implements ApplicationContextFactory {
		public ConfigurableApplicationContext createApplicationContext() {
			StaticApplicationContext context = new StaticApplicationContext();
			context.registerSingleton("job", JobSupport.class);
			return context;
		}

	}

}
