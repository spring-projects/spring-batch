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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.JobRegistryBackgroundJobRunner;
import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.jmx.access.InvalidInvocationException;
import org.springframework.jmx.access.MBeanProxyFactoryBean;
import org.springframework.jmx.support.MBeanServerConnectionFactoryBean;

/**
 * @author Dave Syer
 * 
 */
public class RemoteLauncherTests {

	private static Log logger = LogFactory.getLog(RemoteLauncherTests.class);

	private static List<Exception> errors = new ArrayList<Exception>();

	private static JobOperator launcher;

	private static JobLoader loader;

	static private Thread thread;

	@Test
	public void testConnect() throws Exception {
		String message = errors.isEmpty() ? "" : errors.get(0).getMessage();
		assertEquals(message, 0, errors.size());
		assertTrue(isConnected());
	}

	@Test
	public void testLaunchBadJob() throws Exception {
		assertEquals(0, errors.size());
		assertTrue(isConnected());
		try {
			launcher.start("foo", "time=" + (new Date().getTime()));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			// expected;
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.contains("NoSuchJobException"));
		}
	}

	@Test
	public void testAvailableJobs() throws Exception {
		assertEquals(0, errors.size());
		assertTrue(isConnected());
		assertTrue(launcher.getJobNames().contains("loopJob"));
	}

	@Test
	public void testPauseJob() throws Exception {
		final int SLEEP_INTERVAL = 600;
		assertTrue(isConnected());
		assertTrue(launcher.getJobNames().contains("loopJob"));
		long executionId = launcher.start("loopJob", "");

		// sleep long enough to avoid race conditions (serializable tx isolation
		// doesn't work with HSQL)
		Thread.sleep(SLEEP_INTERVAL);
//		assertEquals(1, launcher.getRunningExecutions("loopJob").size());
		launcher.stop(executionId);

		Thread.sleep(SLEEP_INTERVAL);
//		assertEquals(0, launcher.getRunningExecutions("loopJob").size());
		logger.debug(launcher.getSummary(executionId));
		long resumedId = launcher.restart(executionId);
		assertNotSame("Picked up the same execution after pause and resume", executionId, resumedId);

		Thread.sleep(SLEEP_INTERVAL);
		launcher.stop(resumedId);
		Thread.sleep(SLEEP_INTERVAL);

//		assertEquals(0, launcher.getRunningExecutions("loopJob").size());
		logger.debug(launcher.getSummary(resumedId));
		long resumeId2 = launcher.restart(resumedId);
		assertNotSame("Picked up the same execution after pause and resume", executionId, resumeId2);

		Thread.sleep(SLEEP_INTERVAL);
		launcher.stop(resumeId2);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@BeforeClass
	public static void setUp() throws Exception {
		System.setProperty("com.sun.management.jmxremote", "");
		thread = new Thread(new Runnable() {
			public void run() {
				try {
					JobRegistryBackgroundJobRunner.main("adhoc-job-launcher-context.xml", "jobs/adhocLoopJob.xml");
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

	@AfterClass
	public static void cleanUp() {
		JobRegistryBackgroundJobRunner.stop();
	}

	private static boolean isConnected() throws Exception {
		boolean connected = false;
		if (!JobRegistryBackgroundJobRunner.getErrors().isEmpty()) {
			throw JobRegistryBackgroundJobRunner.getErrors().get(0);
		}
		if (launcher == null) {
			MBeanServerConnectionFactoryBean connectionFactory = new MBeanServerConnectionFactoryBean();
			try {
				launcher = (JobOperator) getMBean(connectionFactory, "spring:service=batch,bean=jobOperator",
						JobOperator.class);
				loader = (JobLoader) getMBean(connectionFactory, "spring:service=batch,bean=jobLoader", JobLoader.class);
			}
			catch (MBeanServerNotFoundException e) {
				// ignore
				return false;
			}
		}
		try {
			launcher.getJobNames();
			connected = loader.getConfigurations().size() > 0;
			logger.info("Configurations loaded: " + loader.getConfigurations());
		}
		catch (InvalidInvocationException e) {
			// ignore
		}
		return connected;
	}

	private static Object getMBean(MBeanServerConnectionFactoryBean connectionFactory, String objectName,
			Class<?> interfaceType) throws MalformedObjectNameException {
		MBeanProxyFactoryBean factory = new MBeanProxyFactoryBean();
		factory.setObjectName(objectName);
		factory.setProxyInterface(interfaceType);
		factory.setServer((MBeanServerConnection) connectionFactory.getObject());
		factory.afterPropertiesSet();
		return factory.getObject();
	}

}
