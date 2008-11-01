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
package org.springframework.batch.sample.launch;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.launch.support.ExportedJobLauncher;
import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.jmx.access.InvalidInvocationException;
import org.springframework.jmx.access.MBeanProxyFactoryBean;
import org.springframework.jmx.support.MBeanServerConnectionFactoryBean;

/**
 * @author Dave Syer
 * 
 */
public class RemoteLauncherTests extends TestCase {
	
	private static Log logger = LogFactory.getLog(RemoteLauncherTests.class);

	private static List errors = new ArrayList();

	private static Thread thread;

	private static ExportedJobLauncher launcher;

	private static JobLoader loader;

	public void testConnect() throws Exception {
		assertEquals(0, errors.size());
		assertTrue(isConnected());
	}

	public void testLaunchBadJob() throws Exception {
		assertEquals(0, errors.size());
		assertTrue(isConnected());
		String result = launcher.run("foo");
		assertTrue("Should contain 'NoSuchJobException': " + result, result.indexOf("NoSuchJobException") >= 0);
	}

	public void testLaunchAndStopRealJob() throws Exception {
		assertEquals(0, errors.size());
		assertTrue(isConnected());
		String result = launcher.run("loopJob");
		assertTrue("Should contain 'JobExecution': " + result, result.indexOf("JobExecution: id=") >= 0);
		Thread.sleep(500);
		assertTrue(launcher.isRunning());
		launcher.stop();
		assertFalse(launcher.isRunning());
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		if (launcher != null) {
			return;
		}
		System.setProperty("com.sun.management.jmxremote", "");
		thread = new Thread(new Runnable() {
			public void run() {
				try {
					TaskExecutorLauncher.main(new String[0]);
				}
				catch (Exception e) {
					errors.add(e);
				}
			}
		});
		thread.start();
		int count = 0;
		while (!isConnected() && count++ < 10) {
			Thread.sleep(1000);
		}
	}

	/**
	 * @throws Exception 
	 * 
	 */
	private static boolean isConnected() throws Exception {
		boolean connected = false;
		if (!TaskExecutorLauncher.getErrors().isEmpty()) {
			throw (RuntimeException) TaskExecutorLauncher.getErrors().get(0);
		}
		if (launcher == null) {
			MBeanServerConnectionFactoryBean connectionFactory = new MBeanServerConnectionFactoryBean();
			connectionFactory.setServiceUrl("service:jmx:rmi://localhost/jndi/rmi://localhost:1099/batch-samples");
			try {
				launcher = (ExportedJobLauncher) getMBean(connectionFactory, "spring:service=batch,bean=jobLauncher", ExportedJobLauncher.class);
				loader = (JobLoader) getMBean(connectionFactory, "spring:service=batch,bean=jobLoader", JobLoader.class);
			}
			catch (MBeanServerNotFoundException e) {
				// ignore
				return false;
			}
		}
		try {
			launcher.isRunning();
			connected = loader.getConfigurations().size()>0;
			logger.info("Configurations loaded: " + loader.getConfigurations());
		}
		catch (InvalidInvocationException e) {
			// ignore
		}
		return connected;
	}

	/**
	 * @param connectionFactory
	 * @param objectName 
	 * @param interfaceType 
	 * @throws MalformedObjectNameException
	 */
	private static Object getMBean(MBeanServerConnectionFactoryBean connectionFactory, String objectName, Class interfaceType)
			throws MalformedObjectNameException {
		MBeanProxyFactoryBean factory = new MBeanProxyFactoryBean();
		factory.setObjectName(objectName);
		factory.setProxyInterface(interfaceType);
		factory.setServer((MBeanServerConnection) connectionFactory.getObject());
		// factory.setServiceUrl("service:jmx:rmi://localhost/jndi/rmi://localhost:1099/batch-samples");
		factory.afterPropertiesSet();
		return factory.getObject();
	}

}
