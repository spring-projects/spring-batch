package org.springframework.batch.integration.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.interceptor.RepeatOperationsInterceptor;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.retry.interceptor.MethodArgumentsKeyGenerator;
import org.springframework.batch.retry.interceptor.MethodInvocationRecoverer;
import org.springframework.batch.retry.interceptor.StatefulRetryOperationsInterceptor;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.context.Lifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.endpoint.SourcePoller;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.scheduling.IntervalTrigger;
import org.springframework.integration.scheduling.SimpleTaskScheduler;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.StringUtils;

public class PollableSourceRetryTests {

	private Log logger = LogFactory.getLog(getClass());

	private List<String> processed = new ArrayList<String>();

	protected List<String> recovered = new ArrayList<String>();

	public void add(String str) {
		logger.debug("Adding: " + str);
		processed.add(str);
	}

	MethodArgumentsKeyGenerator methodArgumentsKeyGenerator = new MethodArgumentsKeyGenerator() {
		@SuppressWarnings("unchecked")
		public Object getKey(Object[] item) {
			if (item == null) {
				return "NULL";
			}
			return ((Message<Object>) item[0]).getPayload();
		}
	};

	private PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();;

	@Test
	public void testSimpleTransactionalPolling() throws Exception {

		List<String> list = TransactionAwareProxyFactory.createTransactionalList();
		list.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,b,c,d,e,f,g,h,j,k")));
		int beforeCount = list.size();

		MessageConsumer handler = new MessageConsumer() {
			public void onMessage(Message<?> message) {
				Object payload = message.getPayload();
				logger.debug("Handling: " + payload);
				processed.add((String) payload);
			}
		};
		MessageSource<Object> source = getPollableSource(list);
		MessageChannel target = getChannel(handler);
		SourcePoller trigger = getSourcePoller(source, target, transactionManager, 1);
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

		MessageConsumer handler = new MessageConsumer() {
			public void onMessage(Message<?> message) {
				Object payload = message.getPayload();
				logger.debug("Handling: " + payload);
				processed.add((String) payload);
				throw new RuntimeException("Planned failure: " + payload);
			}
		};
		MessageSource<Object> source = getPollableSource(list);
		MessageChannel target = getChannel(handler);
		SourcePoller trigger = getSourcePoller(source, target, null, 1);
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

		MessageConsumer handler = new MessageConsumer() {
			public void onMessage(Message<?> message) {
				Object payload = message.getPayload();
				logger.debug("Handling: " + payload);
				processed.add((String) payload);
				throw new RuntimeException("Planned failure: " + payload);
			}
		};
		MessageSource<Object> source = getPollableSource(list);
		MessageChannel target = getChannel(handler);
		SourcePoller trigger = getSourcePoller(source, target, transactionManager, 1);
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

		MessageConsumer handler = new MessageConsumer() {
			public void onMessage(Message<?> message) {
				Object payload = message.getPayload();
				logger.debug("Handling: " + payload);
				processed.add((String) payload);
				if ("fail".equals(payload)) {
					throw new RuntimeException("Planned failure: " + payload);
				}
			}
		};

		MessageSource<Object> source = getPollableSource(list);
		MessageChannel target = getChannel(handler);
		SourcePoller trigger = getSourcePoller(source, target, transactionManager, 1);
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

		MessageConsumer handler = new MessageConsumer() {
			public void onMessage(Message<?> message) {
				Object payload = message.getPayload();
				logger.debug("Handling: " + payload);
				processed.add((String) payload);
				if ("fail".equals(payload)) {
					throw new RuntimeException("Planned failure: " + payload);
				}
			}
		};

		MessageSource<Object> source = getPollableSource(list);
		MessageChannel target = getChannel(handler);
		SourcePoller trigger = getSourcePoller(source, target, null, 1);
		SourcePoller task = (SourcePoller) getProxy(trigger, SourcePoller.class, new Advice[] {
				new TransactionInterceptor(transactionManager, new MatchAlwaysTransactionAttributeSource()),
				getRepeatOperationsInterceptor(3) }, "run");
		TaskScheduler scheduler = getSchedulerWithErrorHandler(task);

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

		MessageConsumer handler = new MessageConsumer() {
			public void onMessage(Message<?> message) {
				if (message == null) {
					return;
				}
				Object payload = message.getPayload();
				logger.debug("Handling: " + payload);
				processed.add((String) payload);
				// INT-184 this won't work if it is a "real" handler that throws
				// MessageHandlingException
				if ("fail".equals(payload)) {
					throw new RuntimeException("Planned failure: " + payload);
				}
			}
		};

		MessageSource<Object> source = getPollableSource(list);
		MessageChannel target = getChannel(handler);
		// this was the old dispatch advice chain
		target = (MessageChannel) getProxy(target, MessageChannel.class,
				new Advice[] { getRetryOperationsInterceptor(methodArgumentsKeyGenerator) }, "send");
		SourcePoller trigger = getSourcePoller(source, target, transactionManager, 1);

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

		MessageConsumer handler = new MessageConsumer() {
			public void onMessage(Message<?> message) {
				Object payload = message.getPayload();
				logger.debug("Handling: " + payload);
				processed.add((String) payload);
				if ("fail".equals(payload)) {
					throw new RuntimeException("Planned failure: " + payload);
				}
			}
		};

		MessageSource<Object> source = getPollableSource(list);
		MessageChannel target = getChannel(handler);

		// this was the old dispatch advice chain
		target = (MessageChannel) getProxy(target, MessageChannel.class,
				new Advice[] { getRetryOperationsInterceptor(methodArgumentsKeyGenerator) }, "send");
		SourcePoller trigger = getSourcePoller(source, target, null, 1);
		SourcePoller task = (SourcePoller) getProxy(trigger, SourcePoller.class, new Advice[] {
			new TransactionInterceptor(transactionManager, new MatchAlwaysTransactionAttributeSource()),
			getRepeatOperationsInterceptor(3) }, "run");
		TaskScheduler scheduler = getSchedulerWithErrorHandler(task);

		waitForResults(scheduler, 6, 100);
		System.err.println(processed);
		System.err.println(list);

		assertFalse("No messages got to processor", processed.isEmpty());
		assertEquals(7, processed.size());
		// 6 items were removed from the list
		assertEquals(beforeCount - 6, list.size());
		assertEquals("a", processed.get(0));
		assertEquals("fail", processed.get(1));
		// retry makes it fail once then recover...
		assertEquals("a", processed.get(2));
		assertEquals("c", processed.get(3));
		assertEquals("d", processed.get(4));

	}

	private SourcePoller getSourcePoller(MessageSource<Object> source, MessageChannel channel,
			PlatformTransactionManager transactionManager, int maxMessagesPerPoll) {
		SourcePoller poller = new SourcePoller(source, channel, new IntervalTrigger(100));
		poller.setTransactionManager(transactionManager);
		poller.setMaxMessagesPerPoll(maxMessagesPerPoll);
		return poller;
	}

	private DirectChannel getChannel(MessageConsumer handler) {
		DirectChannel channel = new DirectChannel();
		channel.setBeanName("input");
		channel.subscribe(handler);
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

	private MessageSource<Object> getPollableSource(List<String> list) {
		final ItemReader<String> reader = new ListItemReader<String>(list) {
			public String read() {
				String item = super.read();
				logger.debug("Reading: " + item);
				return item;
			}
		};
		MessageSource<Object> source = new MessageSource<Object>() {
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

	private TaskScheduler getSchedulerWithErrorHandler(SourcePoller task) {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		executor.setConcurrencyLimit(1);
		TaskScheduler scheduler = new SimpleTaskScheduler(executor);
		scheduler.schedule(task, task.getTrigger());
		return scheduler;
	}

	/**
	 * @param methodArgumentsKeyGenerator
	 * @return
	 */
	private StatefulRetryOperationsInterceptor getRetryOperationsInterceptor(
			MethodArgumentsKeyGenerator methodArgumentsKeyGenerator) {
		StatefulRetryOperationsInterceptor advice = new StatefulRetryOperationsInterceptor();
		advice.setRecoverer(new MethodInvocationRecoverer<Boolean>() {
			@SuppressWarnings("unchecked")
			public Boolean recover(Object[] data, Throwable cause) {
				if (data == null) {
					return false;
				}
				String payload = ((Message<String>) data[0]).getPayload();
				logger.debug("Recovering: " + payload);
				recovered.add(payload);
				return true;
			}
		});
		advice.setKeyGenerator(methodArgumentsKeyGenerator);
		return advice;
	}

	private RepeatOperationsInterceptor getRepeatOperationsInterceptor(int commitInterval) {
		RepeatOperationsInterceptor advice = new RepeatOperationsInterceptor();
		RepeatTemplate repeatTemplate = new RepeatTemplate();
		repeatTemplate.setCompletionPolicy(new SimpleCompletionPolicy(commitInterval));
		advice.setRepeatOperations(repeatTemplate);
		return advice;
	}

	/**
	 * @param commitInterval
	 * @return
	 */
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
