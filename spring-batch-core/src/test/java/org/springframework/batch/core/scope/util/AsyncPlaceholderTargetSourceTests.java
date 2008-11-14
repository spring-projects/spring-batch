package org.springframework.batch.core.scope.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AsyncPlaceholderTargetSourceTests implements BeanFactoryAware {

	private ThreadLocal<Map<String, String>> attributes = new ThreadLocal<Map<String, String>>();

	public Map<String, String> getAttributes() {
		return attributes.get();
	}

	@Autowired
	private Node simple;

	@Autowired
	private SimpleContextFactory contextFactory;

	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	private ListableBeanFactory beanFactory;

	private int beanCount;

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@After
	public void removeContext() {
		contextFactory.clearContext();
		attributes.set(null);
		// Check that all temporary bean definitions are cleaned up
		assertEquals(beanCount, beanFactory.getBeanDefinitionCount());
	}

	@Before
	public void setUpContext() {
		contextFactory.setContext(this);
		beanCount = beanFactory.getBeanDefinitionCount();
	}

	@Test
	public void testGetSimple() {
		attributes.set(Collections.singletonMap("foo", "bar"));
		assertEquals("bar", simple.getName());
	}

	@Test
	public void testGetMultiple() throws Exception {

		List<FutureTask<String>> tasks = new ArrayList<FutureTask<String>>();

		for (int i = 0; i < 12; i++) {
			final String value = "foo" + i;
			FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
				public String call() throws Exception {
					attributes.set(Collections.singletonMap("foo", value));
					try {
						return simple.getName();
					}
					finally {
						attributes.set(null);
					}
				}
			});
			tasks.add(task);
			taskExecutor.execute(task);
		}

		int i = 0;
		for (FutureTask<String> task : tasks) {
			assertEquals("foo" + i, task.get());
			i++;
		}

	}

	public static class SimpleContextFactory extends ContextFactorySupport {

		private Object root;

		public Object getContext() {
			return root;
		}

		public void setContext(Object root) {
			this.root = root;
		}

		public void clearContext() {
			root = null;
		}

	}

	public static interface Node {
		String getName();
	}

	public static class Foo implements Node {

		private String name;

		private Log logger = LogFactory.getLog(getClass());

		public String getName() {
			return name;
		}

		public void setName(String name) {
			logger.debug("Setting name: " + name);
			this.name = name;
		}

	}

}
