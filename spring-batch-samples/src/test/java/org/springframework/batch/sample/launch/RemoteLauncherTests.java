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

import junit.framework.TestCase;

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

	private static List errors = new ArrayList();

	private static Thread thread;

	private static ExportedJobLauncher launcher;

	private static MBeanProxyFactoryBean factory;

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
		MBeanServerConnectionFactoryBean connectionFactory = new MBeanServerConnectionFactoryBean();
		connectionFactory.setServiceUrl("service:jmx:rmi://localhost/jndi/rmi://localhost:1099/batch-samples");
		factory = new MBeanProxyFactoryBean();
		factory.setObjectName("spring:service=batch,bean=jobLauncher");
		factory.setProxyInterface(ExportedJobLauncher.class);
		factory.setServer((MBeanServerConnection) connectionFactory.getObject());
		// factory.setServiceUrl("service:jmx:rmi://localhost/jndi/rmi://localhost:1099/batch-samples");
		int count = 0;
		while (!isConnected() && count++ < 10) {
			Thread.sleep(1000);
		}
	}

	/**
	 * 
	 */
	private static boolean isConnected() {
		boolean connected = false;
		if (!TaskExecutorLauncher.getErrors().isEmpty()) {
			throw (RuntimeException) TaskExecutorLauncher.getErrors().get(0);
		}
		if (launcher == null) {
			try {
				factory.afterPropertiesSet();
				launcher = (ExportedJobLauncher) factory.getObject();
			}
			catch (MBeanServerNotFoundException e) {
				// ignore
				return false;
			}
		}
		try {
			launcher.isRunning();
			connected = true;
		}
		catch (InvalidInvocationException e) {
			// ignore
		}
		return connected;
	}

}
