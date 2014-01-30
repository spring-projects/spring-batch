/*
 * Copyright 2006-2014 the original author or authors.
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

package org.springframework.batch.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

public class SpringBeanJobTests {

	@Test
	public void testBeanName() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		JobSupport configuration = new JobSupport();
		context.getAutowireCapableBeanFactory().initializeBean(configuration,
				"bean");
		context.refresh();
		assertNotNull(configuration.getName());
		configuration.setBeanName("foo");
		context.getAutowireCapableBeanFactory().initializeBean(configuration,
				"bean");
		assertEquals("bean", configuration.getName());
		context.close();
	}

	@Test
	public void testBeanNameWithBeanDefinition() throws Exception {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues args = new ConstructorArgumentValues();
		args.addGenericArgumentValue("foo");
		context.registerBeanDefinition("bean", new RootBeanDefinition(
				JobSupport.class, args, null));

		context.refresh();
		JobSupport configuration = (JobSupport) context
				.getBean("bean");
		assertNotNull(configuration.getName());
		assertEquals("foo", configuration.getName());
		configuration.setBeanName("bar");
		assertEquals("foo", configuration.getName());
		context.close();
	}

	@Test
	public void testBeanNameWithParentBeanDefinition() throws Exception {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues args = new ConstructorArgumentValues();
		args.addGenericArgumentValue("bar");
		context.registerBeanDefinition("parent", new RootBeanDefinition(
				JobSupport.class, args, null));
		context.registerBeanDefinition("bean", new ChildBeanDefinition("parent"));
		context.refresh();
		JobSupport configuration = (JobSupport) context
				.getBean("bean");
		assertNotNull(configuration.getName());
		assertEquals("bar", configuration.getName());
		configuration.setBeanName("foo");
		assertEquals("bar", configuration.getName());
		configuration.setName("foo");
		assertEquals("foo", configuration.getName());
		context.close();
	}
}
