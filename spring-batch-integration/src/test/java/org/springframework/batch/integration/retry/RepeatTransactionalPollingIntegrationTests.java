/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.integration.retry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.StringUtils;

@SpringJUnitConfig
@MessageEndpoint
class RepeatTransactionalPollingIntegrationTests implements ApplicationContextAware {

	private final Log logger = LogFactory.getLog(getClass());

	private static final List<String> processed = new ArrayList<>();

	private static List<String> expected;

	private static final List<String> handled = new ArrayList<>();

	private static List<String> list = new ArrayList<>();

	private Lifecycle bus;

	private volatile static int count = 0;

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		bus = (Lifecycle) applicationContext;
	}

	public String process(String message) {
		String result = message + ": " + count;
		logger.debug("Handling: " + message);
		if (count < expected.size()) {
			processed.add(message);
			count++;
		}
		if ("fail".equals(message)) {
			throw new RuntimeException("Planned failure");
		}
		return result;
	}

	public String input() {
		logger.debug("Polling: " + count);
		if (list.isEmpty()) {
			return null;
		}
		return list.remove(0);
	}

	public void output(String message) {
		handled.add(message);
		logger.debug("Handled: " + message);
	}

	@BeforeEach
	void clearLists() {
		list.clear();
		handled.clear();
		processed.clear();
		count = 0;
	}

	@Test
	@DirtiesContext
	void testSunnyDay() throws Exception {
		list = TransactionAwareProxyFactory.createTransactionalList(
				Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,c,d,e,f,g,h,j,k")));
		expected = Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,c,d"));
		waitForResults(bus, expected.size(), 60);
		assertEquals(expected, processed);
	}

	@Test
	@DirtiesContext
	void testRollback() throws Exception {
		list = TransactionAwareProxyFactory.createTransactionalList(
				Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,fail,d,e,f,g,h,j,k")));
		expected = Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,fail,fail"));
		waitForResults(bus, expected.size(), 60);
		assertEquals(expected, processed);
		assertEquals(2, handled.size()); // a,b
	}

	private void waitForResults(Lifecycle lifecycle, int count, int maxTries) throws InterruptedException {
		lifecycle.start();
		int timeout = 0;
		while (processed.size() < count && timeout++ < maxTries) {
			Thread.sleep(5);
		}
		lifecycle.stop();
	}

}
