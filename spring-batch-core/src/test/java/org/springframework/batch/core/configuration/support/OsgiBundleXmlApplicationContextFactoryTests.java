/*
 * Copyright 2006-2007 the original author or authors.
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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
public class OsgiBundleXmlApplicationContextFactoryTests {
	
	private OsgiBundleXmlApplicationContextFactory factory = new OsgiBundleXmlApplicationContextFactory();

	/**
	 * Test method for {@link org.springframework.batch.core.configuration.support.OsgiBundleXmlApplicationContextFactory#setDisplayName(java.lang.String)}.
	 */
	@Test
	public void testSetDisplayName() {
		factory.setDisplayName("foo");
		factory.setPath("classpath:"+ClassUtils.addResourcePathToPackagePath(getClass(), "trivial-context.xml"));
		BundleContext bundleContext = createMock(BundleContext.class);
		Bundle bundle = createNiceMock(Bundle.class);
		expect(bundleContext.getBundle()).andReturn(bundle).anyTimes();
		replay(bundleContext, bundle);
		factory.setBundleContext(bundleContext);
		// factory.createApplicationContext();
		verify(bundleContext, bundle);
	}

	@Test
	public void testEquals() throws Exception {
		factory.setPath("child-context.xml");
		OsgiBundleXmlApplicationContextFactory other = new OsgiBundleXmlApplicationContextFactory();
		other.setPath("child-context.xml");
		assertEquals(other, factory);
		assertEquals(other.hashCode(), factory.hashCode());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.configuration.support.OsgiBundleXmlApplicationContextFactory#setApplicationContext(org.springframework.context.ApplicationContext)}.
	 */
	@Test
	public void testSetApplicationContext() {
	}

}
