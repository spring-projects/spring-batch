/*
 * Copyright 2010-2023 the original author or authors.
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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.support.transaction.TransactionAwareProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.StringUtils;

@SpringJUnitConfig
public class RetryTransactionalPollingIntegrationTests implements ApplicationContextAware {

	private final Log logger = LogFactory.getLog(getClass());

	private static List<String> list = new ArrayList<>();

	@Autowired
	private SimpleRecoverer recoverer;

	@Autowired
	private SimpleService service;

	private Lifecycle bus;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		bus = (Lifecycle) applicationContext;
	}

	private static final AtomicInteger count = new AtomicInteger(0);

	@BeforeEach
	void clearLists() {
		list.clear();
		count.set(0);
	}

	public @Nullable String input() {
		logger.debug("Polling: " + count);
		if (list.isEmpty()) {
			return null;
		}
		// This happens in a transaction and the list is transactional so if it rolls back
		// the same item comes back
		// again at the head of the list
		return list.remove(0);
	}

	public void output(String message) {
		count.incrementAndGet();
		logger.debug("Handled: " + message);
	}

	@Test
	@DirtiesContext
	void testSunnyDay() throws Exception {
		list = TransactionAwareProxyFactory
			.createTransactionalList(Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,c,d,e,f,g,h,j,k")));
		List<String> expected = Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,c,d"));
		service.setExpected(expected);
		waitForResults(bus, expected.size(), 60);
		assertEquals(4, service.getProcessed().size()); // a,b,c,d
		assertEquals(expected, service.getProcessed());
	}

	@Test
	@DirtiesContext
	void testRollback() throws Exception {
		list = TransactionAwareProxyFactory.createTransactionalList(
				Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,fail,d,e,f,g,h,j,k")));

		List<String> expected = Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,fail,fail,d,e"));
		service.setExpected(expected);
		waitForResults(bus, expected.size(), 100); // a, b, (fail, fail, [fail]), d, e
		assertEquals(6, service.getProcessed().size()); // a,b,fail,fail,d,e
		assertEquals(1, recoverer.getRecovered().size()); // fail
		assertEquals(expected, service.getProcessed());
	}

	private void waitForResults(Lifecycle lifecycle, int count, int maxTries) throws InterruptedException {
		lifecycle.start();
		int timeout = 0;
		while (service.getProcessed().size() < count && timeout++ < maxTries) {
			Thread.sleep(10);
		}
		lifecycle.stop();
	}

}
