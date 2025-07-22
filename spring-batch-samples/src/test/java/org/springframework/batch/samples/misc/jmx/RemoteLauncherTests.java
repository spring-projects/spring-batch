/*
 * Copyright 2006-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.samples.misc.jmx;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.samples.launch.JobLoader;
import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.jmx.access.InvalidInvocationException;
import org.springframework.jmx.access.MBeanProxyFactoryBean;
import org.springframework.jmx.support.MBeanServerConnectionFactoryBean;

import javax.management.MalformedObjectNameException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Dave Syer
 * @author Jinwoo Bae
 * @author Mahmoud Ben Hassine
 *
 */
@SuppressWarnings("removal")
class RemoteLauncherTests {

	private static final Log logger = LogFactory.getLog(RemoteLauncherTests.class);

	private static final List<Exception> errors = new ArrayList<>();

	private static JobOperator launcher;

	private static JobLoader loader;

	@Test
	void testConnect() throws Exception {
		String message = errors.isEmpty() ? "" : errors.get(0).getMessage();

		if (!errors.isEmpty()) {
			fail(message);
		}

		assertTrue(isConnected());
	}

	@Test
	void testLaunchBadJob() throws Exception {
		Properties properties = new Properties();
		properties.setProperty("time", String.valueOf(new Date().getTime()));
		assertEquals(0, errors.size());
		assertTrue(isConnected());

		Exception exception = assertThrows(RuntimeException.class, () -> launcher.start("foo", properties));
		String message = exception.getMessage();
		assertTrue(message.contains("NoSuchJobException"), "Wrong message: " + message);
	}

	@Test
	void testAvailableJobs() throws Exception {
		assertEquals(0, errors.size());
		assertTrue(isConnected());
		assertTrue(launcher.getJobNames().contains("loopJob"));
	}

	@Test
	void testPauseJob() throws Exception {
		final int SLEEP_INTERVAL = 600;

		assertTrue(isConnected());
		assertTrue(launcher.getJobNames().contains("loopJob"));

		long executionId = launcher.start("loopJob", new Properties());

		// sleep long enough to avoid race conditions (serializable tx isolation
		// doesn't work with HSQL)
		Thread.sleep(SLEEP_INTERVAL);

		launcher.stop(executionId);

		Thread.sleep(SLEEP_INTERVAL);

		logger.debug(launcher.getSummary(executionId));
		long resumedId = launcher.restart(executionId);
		assertNotSame(executionId, resumedId, "Picked up the same execution after pause and resume");

		Thread.sleep(SLEEP_INTERVAL);
		launcher.stop(resumedId);
		Thread.sleep(SLEEP_INTERVAL);

		logger.debug(launcher.getSummary(resumedId));
		long resumeId2 = launcher.restart(resumedId);
		assertNotSame(executionId, resumeId2, "Picked up the same execution after pause and resume");

		Thread.sleep(SLEEP_INTERVAL);
		launcher.stop(resumeId2);
	}

	@BeforeAll
	static void setUp() throws Exception {
		System.setProperty("com.sun.management.jmxremote", "");

		Thread thread = new Thread(() -> {
			try {
				JobRegistryBackgroundJobRunner.main(
						"org/springframework/batch/samples/misc/jmx/adhoc-job-launcher-context.xml",
						"org/springframework/batch/samples/misc/jmx/adhocLoopJob.xml");
			}
			catch (Exception e) {
				logger.error(e);
				errors.add(e);
			}
		});

		thread.start();
		int count = 0;

		while (!isConnected() && count++ < 10) {
			Thread.sleep(1000);
		}
	}

	@AfterAll
	static void cleanUp() {
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
				loader = (JobLoader) getMBean(connectionFactory, "spring:service=batch,bean=jobLoader",
						JobLoader.class);
			}
			catch (MBeanServerNotFoundException e) {
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
		factory.setServer(connectionFactory.getObject());
		factory.afterPropertiesSet();
		return factory.getObject();
	}

}
