package org.springframework.batch.integration.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.retry.interceptor.StatefulRetryOperationsInterceptor;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.context.Lifecycle;
import org.springframework.integration.dispatcher.DirectChannel;
import org.springframework.integration.dispatcher.PollingDispatcher;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageExchangeTemplate;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.SchedulableTask;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.integration.scheduling.spi.ProviderTaskScheduler;
import org.springframework.integration.scheduling.spi.SimpleScheduleServiceProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

public class PollableSourceRetryTests {

	private Log logger = LogFactory.getLog(getClass());

	private List<String> processed = new ArrayList<String>();

	protected List<String> recovered = new ArrayList<String>();

	public void add(String str) {
		logger.debug("Adding: " + str);
		processed.add(str);
	}

	ItemKeyGenerator itemKeyGenerator = new ItemKeyGenerator() {
		@SuppressWarnings("unchecked")
		public Object getKey(Object item) {
			if (item == null) {
				return "NULL";
			}
			if (item.getClass().isArray()) {
				item = ((Object[]) item)[0];
			}
			return ((Message<Object>) item).getPayload();
		}
	};

	private PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();;

	@Test
	public void testSimpleTransactionalPolling() throws Exception {

		List<String> list = TransactionAwareProxyFactory.createTransactionalList();
		list.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,c,d,e,f,g,h,j,k")));
		int beforeCount = list.size();

		MessageTarget handler = new MessageTarget() {
			public boolean send(Message<?> message) {
				Object payload = message.getPayload();
				logger.debug("Handling: " + payload);
				return processed.add((String) payload);
			}
		};
		PollableSource<Object> source = getPollableSource(list);
		MessageTarget target = getChannel(handler);
		PollingDispatcher trigger = getPollingDispatcher(source, target, transactionManager, 1);
		TaskScheduler scheduler = getSchedulerWithErrorHandler(trigger);

		waitForResults(scheduler, 2, 40);

		assertEquals(2, processed.size());

		assertEquals(beforeCount - list.size(), processed.size());
		assertEquals("a", processed.get(0));

	}

	@Test
	public void testNonTransactionalPollingWithRollback() throws Exception {

		List<String> list = TransactionAwareProxyFactory.createTransactionalList();
		list.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,c,d,e,f,g,h,j,k")));
		int beforeCount = list.size();

		MessageTarget handler = new MessageTarget() {
			public boolean send(Message<?> message) {
				Object payload = message.getPayload();
				logger.debug("Handling: " + payload);
				processed.add((String) payload);
				throw new RuntimeException("Planned failure: " + payload);
			}
		};
		PollableSource<Object> source = getPollableSource(list);
		MessageTarget target = getChannel(handler);
		PollingDispatcher trigger = getPollingDispatcher(source, target, null, 1);
		TaskScheduler scheduler = getSchedulerWithErrorHandler(trigger);

		waitForResults(scheduler, 2, 20);

		assertEquals(2, processed.size());

		// None rolled back because there was no transaction
		assertEquals(beforeCount - list.size(), 2);
		assertEquals("a", processed.get(0));
		assertEquals("b", processed.get(1));

	}

	@Test
	public void testTransactionalHandlingWithUnconditionalRollback() throws Exception {

		List<String> list = TransactionAwareProxyFactory.createTransactionalList();
		list.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,c,d,e,f,g,h,j,k")));
		int beforeCount = list.size();

		MessageTarget handler = new MessageTarget() {
			public boolean send(Message<?> message) {
				Object payload = message.getPayload();
				logger.debug("Handling: " + payload);
				processed.add((String) payload);
				throw new RuntimeException("Planned failure: " + payload);
			}
		};
		PollableSource<Object> source = getPollableSource(list);
		MessageTarget target = getChannel(handler);
		PollingDispatcher trigger = getPollingDispatcher(source, target, transactionManager, 1);
		TaskScheduler scheduler = getSchedulerWithErrorHandler(trigger);

		waitForResults(scheduler, 2, 40);

		assertEquals(2, processed.size());

		// TODO: this would fail if exception not propagated: INT-184.
		// All rolled back
		assertEquals(beforeCount - list.size(), 0);
		assertEquals("a", processed.get(0));
		// processed twice and rolled back both times
		assertEquals("a", processed.get(1));

	}

	@Test
	public void testTransactionalHandlingWithRollback() throws Exception {

		List<String> list = TransactionAwareProxyFactory.createTransactionalList();
		list.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,fail,d,e,f,g,h,j,k")));
		int beforeCount = list.size();

		MessageTarget handler = new MessageTarget() {
			public boolean send(Message<?> message) {
				Object payload = message.getPayload();
				logger.debug("Handling: " + payload);
				boolean result = processed.add((String) payload);
				if ("fail".equals(payload)) {
					throw new RuntimeException("Planned failure: " + payload);
				}
				return result;
			}
		};

		PollableSource<Object> source = getPollableSource(list);
		MessageTarget target = getChannel(handler);
		PollingDispatcher trigger = getPollingDispatcher(source, target, transactionManager, 1);
		TaskScheduler scheduler = getSchedulerWithErrorHandler(trigger);

		waitForResults(scheduler, 5, 50);

		assertEquals(5, processed.size());
		assertFalse("No messages got to processor", processed.isEmpty());
		// First two TX succeed, and the rest rolled back so list has had two
		// elements popped off
		assertEquals(beforeCount - 2, list.size());
		assertEquals("a", processed.get(0));
		assertEquals("b", processed.get(1));
		// stuck in effectively an infinite loop - it fails every time...
		assertEquals("fail", processed.get(2));
		assertEquals("fail", processed.get(3));
		assertEquals("fail", processed.get(4));

	}

	@Test
	public void testTransactionalHandlingWithRepeat() throws Exception {

		List<String> list = TransactionAwareProxyFactory.createTransactionalList();
		list.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,fail,d,e,f,g,h,j,k")));
		int beforeCount = list.size();

		MessageTarget handler = new MessageTarget() {
			public boolean send(Message<?> message) {
				Object payload = message.getPayload();
				logger.debug("Handling: " + payload);
				boolean result = processed.add((String) payload);
				if ("fail".equals(payload)) {
					throw new RuntimeException("Planned failure: " + payload);
				}
				return result;
			}
		};

		PollableSource<Object> source = getPollableSource(list);
		MessageTarget target = getChannel(handler);
		PollingDispatcher trigger = getPollingDispatcher(source, target, transactionManager, 3);
		TaskScheduler scheduler = getSchedulerWithErrorHandler(trigger);

		waitForResults(scheduler, 6, 100);

		assertEquals(6, processed.size());
		assertFalse("No messages got to processor", processed.isEmpty());
		// Two TX rolled back so list is same size as when it started
		assertEquals(beforeCount, list.size());
		assertEquals("a", processed.get(0));
		assertEquals("b", processed.get(1));
		// stuck in effectively an infinite loop - it fails every time with the
		// same 3 records...
		assertEquals("fail", processed.get(2));
		assertEquals("a", processed.get(3));
		assertEquals("b", processed.get(4));

	}

	@Test
	public void testTransactionalHandlingWithRetry() throws Exception {

		List<String> list = TransactionAwareProxyFactory.createTransactionalList();
		list.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,fail,d,e,f,g,h,j,k")));
		int beforeCount = list.size();

		MessageTarget handler = new MessageTarget() {
			public boolean send(Message<?> message) {
				if (message == null) {
					return false;
				}
				Object payload = message.getPayload();
				logger.debug("Handling: " + payload);
				boolean result = processed.add((String) payload);
				// INT-184 this won't work if it is a "real" handler that throws
				// MessageHandlingException
				if ("fail".equals(payload)) {
					throw new RuntimeException("Planned failure: " + payload);
				}
				return result;
			}
		};

		PollableSource<Object> source = getPollableSource(list);
		MessageTarget target = getChannel(handler);
		// this was the old dispatch advice chain
		target = (MessageTarget) getProxy(target, MessageTarget.class,
				new Advice[] { getRetryOperationsInterceptor(itemKeyGenerator) }, "send");
		PollingDispatcher trigger = getPollingDispatcher(source, target, transactionManager, 1);
		TaskScheduler scheduler = getSchedulerWithErrorHandler(trigger);

		waitForResults(scheduler, 4, 40);

		assertEquals(4, processed.size());
		assertEquals(1, recovered.size());
		assertFalse("No messages got to processor", processed.isEmpty());
		// 4 items from list should have been processed (with no repeats, since
		// the failed item was recovered with no retry - NeverRetryPolicy)
		assertEquals(beforeCount - 4, list.size());
		assertEquals("a", processed.get(0));
		assertEquals("b", processed.get(1));
		// retry makes it fail once then recover...
		assertEquals("fail", processed.get(2));
		assertEquals("d", processed.get(3));

	}

	@Test
	public void testTransactionalHandlingWithRepeatAndRetry() throws Exception {

		List<String> list = TransactionAwareProxyFactory.createTransactionalList();
		list.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,fail,c,d,e,f,g,h,j,k")));
		int beforeCount = list.size();

		MessageTarget handler = new MessageTarget() {
			public boolean send(Message<?> message) {
				Object payload = message.getPayload();
				logger.debug("Handling: " + payload);
				boolean result = processed.add((String) payload);
				if ("fail".equals(payload)) {
					throw new RuntimeException("Planned failure: " + payload);
				}
				return result;
			}
		};

		PollableSource<Object> source = getPollableSource(list);
		MessageTarget target = getChannel(handler);
		// this was the old dispatch advice chain
		target = (MessageTarget) getProxy(target, MessageTarget.class,
				new Advice[] { getRetryOperationsInterceptor(itemKeyGenerator) }, "send");
		PollingDispatcher trigger = getPollingDispatcher(source, target, transactionManager, 3);
		TaskScheduler scheduler = getSchedulerWithErrorHandler(trigger);

		waitForResults(scheduler, 6, 100);
		System.err.println(processed);
		System.err.println(list);

		assertEquals(6, processed.size());
		assertFalse("No messages got to processor", processed.isEmpty());
		// One roll back and then start again with a,b,d,e
		assertEquals(beforeCount - 5, list.size());
		assertEquals("a", processed.get(0));
		assertEquals("fail", processed.get(1));
		// retry makes it fail once then recover...
		assertEquals("a", processed.get(2));
		assertEquals("c", processed.get(3));
		assertEquals("d", processed.get(4));

	}

	private PollingDispatcher getPollingDispatcher(PollableSource<Object> source, MessageTarget target,
			PlatformTransactionManager transactionManager, int commitInterval) {
		MessageExchangeTemplate template = getExchangeTemplate(transactionManager);
		PollingDispatcher dispatcher = new PollingDispatcher(source, new PollingSchedule(100), null, template);
		dispatcher.setMaxMessagesPerPoll(commitInterval);
		dispatcher.subscribe(target);
		return dispatcher;
	}

	private MessageExchangeTemplate getExchangeTemplate(PlatformTransactionManager transactionManager) {
		MessageExchangeTemplate template = new MessageExchangeTemplate();
		template.setTransactionManager(transactionManager);
		template.afterPropertiesSet();
		return template;
	}

	private DirectChannel getChannel(MessageTarget target) {
		DirectChannel channel = new DirectChannel();
		channel.setName("input");
		channel.subscribe(target);
		return channel;
	}

	private void waitForResults(Lifecycle lifecycle, int count, int maxTries) throws InterruptedException {
		lifecycle.start();
		int timeout = 0;
		while (processed.size() < count && timeout++ < maxTries) {
			Thread.sleep(10);
		}
		lifecycle.stop();
	}

	private PollableSource<Object> getPollableSource(List<String> list) {
		final ItemReader<String> reader = new ListItemReader<String>(list) {
			public String read() {
				String item = super.read();
				logger.debug("Reading: " + item);
				return item;
			}
		};
		PollableSource<Object> source = new PollableSource<Object>() {
			public Message<Object> receive() {
				try {
					String payload = reader.read();
					if (payload == null)
						return null;
					return new GenericMessage<Object>(payload);
				}
				catch (RuntimeException e) {
					throw e;
				}
				catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}
		};
		return source;
	}

	private TaskScheduler getSchedulerWithErrorHandler(SchedulableTask task) {
		SimpleScheduleServiceProvider provider = new SimpleScheduleServiceProvider(Executors
				.newSingleThreadScheduledExecutor());
		TaskScheduler scheduler = new ProviderTaskScheduler(provider);
		scheduler.schedule(task);
		return scheduler;
	}

	/**
	 * @param itemKeyGenerator
	 * @return
	 */
	private StatefulRetryOperationsInterceptor getRetryOperationsInterceptor(ItemKeyGenerator itemKeyGenerator) {
		StatefulRetryOperationsInterceptor advice = new StatefulRetryOperationsInterceptor();
		advice.setRecoverer(new ItemRecoverer() {
			@SuppressWarnings("unchecked")
			public Object recover(Object data, Throwable cause) {
				if (data == null) {
					return false;
				}
				if (data.getClass().isArray()) {
					data = ((Object[]) data)[0];
				}
				String payload = ((Message<String>) data).getPayload();
				logger.debug("Recovering: " + payload);
				recovered.add(payload);
				return true;
			}
		});
		advice.setKeyGenerator(itemKeyGenerator);
		return advice;
	}


	/**
	 * @param commitInterval
	 * @return
	 */
	// private RepeatOperationsInterceptor getRepeatOperationsInterceptor(int
	// commitInterval) {
	// RepeatOperationsInterceptor advice = new RepeatOperationsInterceptor();
	// RepeatTemplate repeatTemplate = new RepeatTemplate();
	// repeatTemplate.setCompletionPolicy(new
	// SimpleCompletionPolicy(commitInterval));
	// advice.setRepeatOperations(repeatTemplate);
	// return advice;
	// }

	private Object getProxy(Object target, Class<?> intf, Advice[] advices, String methodName) {
		ProxyFactory factory = new ProxyFactory(target);
		for (int i = 0; i < advices.length; i++) {
			DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(advices[i]);
			NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
			pointcut.addMethodName(methodName);
			advisor.setPointcut(pointcut);
			factory.addAdvisor(advisor);
		}
		factory.setProxyTargetClass(false);
		factory.addInterface(intf);
		return factory.getProxy();
	}

}
