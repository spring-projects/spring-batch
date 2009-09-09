package org.springframework.batch.core.configuration.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
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
	public void testPostProcessing() throws Exception {
		ApplicationContextJobFactory factory = new ApplicationContextJobFactory("job",
				new PostProcessingApplicationContextFactory());
		assertEquals("bar", factory.getJobName());
	}

	private static class StubApplicationContextFactory implements ApplicationContextFactory {
		public ConfigurableApplicationContext createApplicationContext() {
			StaticApplicationContext context = new StaticApplicationContext();
			context.registerSingleton("job", JobSupport.class);
			return context;
		}

	}

	private static class PostProcessingApplicationContextFactory implements ApplicationContextFactory {
		public ConfigurableApplicationContext createApplicationContext() {
			StaticApplicationContext context = new StaticApplicationContext();
			context.registerSingleton("job", JobSupport.class);
			context.registerSingleton("postProcessor", TestBeanPostProcessor.class);
			context.refresh();
			return context;
		}

	}

	private static class TestBeanPostProcessor implements BeanPostProcessor {

		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof JobSupport) {
				((JobSupport) bean).setName("bar");
			}
			return bean;
		}

		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

	}

}
