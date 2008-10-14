package org.springframework.batch.integration.retry;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.Lifecycle;
import org.springframework.integration.annotation.ChannelAdapter;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.bus.MessageBus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@MessageEndpoint
public class RetryTransactionalPollingIntegrationTests {

	private Log logger = LogFactory.getLog(getClass());

	private List<String> list = new ArrayList<String>();

	@Autowired
	private SimpleRecoverer recoverer;

	@Autowired
	private SimpleService service;

	@Autowired
	private MessageBus bus;

	private volatile int count = 0;
	
	@ChannelAdapter("requests")
	@Poller(interval=10, transactionManager="transactionManager")
	public String input() {
		logger.debug("Polling: " + count);
		if (list.isEmpty()) {
			return null;
		}
		return list.remove(0);
	}

	@ChannelAdapter("replies")
	public void output(String message) {	
		count++;
		logger.debug("Handled: " + message);		
	}

	@Test
	@DirtiesContext
	public void testSunnyDay() throws Exception {
		list = TransactionAwareProxyFactory.createTransactionalList(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("a,b,c,d,e,f,g,h,j,k")));
		waitForResults(bus, 4, 60);
		assertEquals(4,service.getProcessed().size()); // a,b,c,d
		assertEquals(4,count);
	}

	@Test
	@DirtiesContext
	public void testRollback() throws Exception {
		list = TransactionAwareProxyFactory.createTransactionalList(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("a,b,fail,d,e,f,g,h,j,k")));
		waitForResults(bus, 6, 200); // (a), (b), (fail), (fail), ...
		System.err.println(service.getProcessed());
		System.err.println(recoverer.getRecovered());
		assertEquals(6,service.getProcessed().size()); // a,b,fail,fail,d,e
		assertEquals(1,recoverer.getRecovered().size()); // fail
		assertEquals(4,count); // a,b,d,e
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
