/*
 * Copyright 2006-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.configuration.support;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Dave Syer
 * 
 */
public class DefaultJobLoaderTests {

	private JobRegistry registry = new MapJobRegistry();

	private DefaultJobLoader jobLoader = new DefaultJobLoader(registry);

	@Test
	public void testLoadWithExplicitName() throws Exception {
		ClassPathXmlApplicationContextFactory factory = new ClassPathXmlApplicationContextFactory(
				new ByteArrayResource(JOB_XML.getBytes()));
		jobLoader.load(factory);
		assertEquals(1, registry.getJobNames().size());
		jobLoader.reload(factory);
		assertEquals(1, registry.getJobNames().size());
	}

	@Test
	public void testReload() throws Exception {
		ClassPathXmlApplicationContextFactory factory = new ClassPathXmlApplicationContextFactory(
				new ClassPathResource("trivial-context.xml", getClass()));
		jobLoader.load(factory);
		assertEquals(1, registry.getJobNames().size());
		jobLoader.reload(factory);
		assertEquals(1, registry.getJobNames().size());
	}

	@Test
	public void testReloadWithAutoRegister() throws Exception {
		ClassPathXmlApplicationContextFactory factory = new ClassPathXmlApplicationContextFactory(
				new ClassPathResource("trivial-context-autoregister.xml", getClass()));
		jobLoader.load(factory);
		assertEquals(1, registry.getJobNames().size());
		jobLoader.reload(factory);
		assertEquals(1, registry.getJobNames().size());
	}

	private static final String JOB_XML = String
			.format(
					"<beans xmlns='http://www.springframework.org/schema/beans' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
							+ "xsi:schemaLocation='http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.5.xsd'><bean class='%s$StubJob'/></beans>",
					DefaultJobLoaderTests.class.getName());

	public static class StubJob implements Job {

		public void execute(JobExecution execution) {
		}

		public JobParametersIncrementer getJobParametersIncrementer() {
			return null;
		}

		public String getName() {
			return "job";
		}

		public boolean isRestartable() {
			return false;
		}

		public JobParametersValidator getJobParametersValidator() {
			return null;
		}

	}

}
