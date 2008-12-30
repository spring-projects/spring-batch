package org.springframework.batch.integration.retry;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@MessageEndpoint
public class TransactionalPollingIntegrationTests implements ApplicationContextAware {

	private Log logger = LogFactory.getLog(getClass());

	private static List<String> processed = new ArrayList<String>();

	private static List<String> handled = new ArrayList<String>();

	private static List<String> expected = new ArrayList<String>();

	private static List<String> list = new ArrayList<String>();

	private Lifecycle bus;

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		bus = (Lifecycle) applicationContext;
	}

	private volatile static int count = 0;

	@ServiceActivator(inputChannel = "requests", outputChannel = "replies")
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
		logger.debug("Polling: " + count + " of " + list.size());
		if (list.isEmpty()) {
			return null;
		}
		return list.remove(0);
	}

	public void output(String message) {
		if (count < expected.size()) {
			handled.add(message);
		}
		logger.debug("Handled: " + message);
	}

	@Before
	public void clearLists() {
		list.clear();
		handled.clear();
		processed.clear();
		count = 0;
	}

	@Test
	@DirtiesContext
	public void testSunnyDay() throws Exception {
		list = TransactionAwareProxyFactory.createTransactionalList(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("a,b,c,d,e,f,g,h,j,k")));
		expected = Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,c,d"));
		waitForResults(bus, 4, 60);
		assertEquals(expected, processed);
	}

	@Test
	@DirtiesContext
	public void testRollback() throws Exception {
		list = TransactionAwareProxyFactory.createTransactionalList(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("a,b,fail,d,e,f,g,h,j,k")));
		expected = Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,fail,fail"));
		waitForResults(bus, 4, 30);
		assertEquals(expected, processed);
		assertEquals(2, handled.size()); // a,b
	}

	private void waitForResults(Lifecycle lifecycle, int count, int maxTries) throws InterruptedException {
		lifecycle.start();
		int timeout = 0;
		while (processed.size() < count && timeout++ < maxTries) {
			Thread.sleep(10);
		}
		lifecycle.stop();
	}

}
