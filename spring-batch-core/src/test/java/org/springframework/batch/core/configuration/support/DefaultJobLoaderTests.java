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
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Dave Syer
 * 
 */
public class DefaultJobLoaderTests {

	private JobRegistry registry = new MapJobRegistry();

	private DefaultJobLoader jobLoader = new DefaultJobLoader(registry);

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

}
