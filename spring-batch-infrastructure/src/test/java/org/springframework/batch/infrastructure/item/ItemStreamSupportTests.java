/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.infrastructure.item;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.aop.framework.ProxyFactoryBean;

/**
 * @author Jimmy Praet
 */
public class ItemStreamSupportTests {

	private SimpleItemStreamSupport itemStream = new SimpleItemStreamSupport();

	@Test
	public void testDefaultName() {
		assertEquals("ItemStreamSupportTests.SimpleItemStreamSupport.foo", itemStream.getExecutionContextKey("foo"));
	}

	@Test
	public void testDefaultNameForCglibProxy() {
		ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
		proxyFactoryBean.setProxyTargetClass(true);
		proxyFactoryBean.setTarget(itemStream);
		itemStream = (SimpleItemStreamSupport) proxyFactoryBean.getObject();
		assertEquals("ItemStreamSupportTests.SimpleItemStreamSupport.foo", itemStream.getExecutionContextKey("foo"));
	}

	@Test
	public void testSetName() {
		itemStream.setName("name");
		assertEquals("name.foo", itemStream.getExecutionContextKey("foo"));
	}

	@Test
	public void testSetExecutionContextName() {
		itemStream.setExecutionContextName("name");
		assertEquals("name.foo", itemStream.getExecutionContextKey("foo"));
	}

	@Test
	public void testBeanName() {
		itemStream.setBeanName("beanName");
		assertEquals("beanName.foo", itemStream.getExecutionContextKey("foo"));
	}

	@Test
	public void testExplicitNameTakesPrecedenceOverBeanName() {
		itemStream.setName("explicitName");
		itemStream.setBeanName("beanName");
		assertEquals("explicitName.foo", itemStream.getExecutionContextKey("foo"));
	}

	public static class SimpleItemStreamSupport extends ItemStreamSupport {

	}

}