package org.springframework.batch.core.scope;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StopWatch;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class StepScopePerformanceTests implements ApplicationContextAware {
	
	private Log logger = LogFactory.getLog(getClass());

	private ApplicationContext applicationContext;

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;

	}

	@Before
	public void start() throws Exception {
		int count = doTest("vanilla", "warmup");
		logger.info("Item count: "+count);
		StepSynchronizationManager.register(new StepExecution("step", new JobExecution(0L),1L));
	}

	@Before
	@After
	public void cleanup() {
		StepSynchronizationManager.close();
	}

	@Test
	public void testVanilla() throws Exception {
		int count = doTest("vanilla", "vanilla");
		logger.info("Item count: "+count);
	}

	@Test
	public void testProxied() throws Exception {
		int count = doTest("proxied", "proxied");
		logger.info("Item count: "+count);
	}

	private int doTest(String name, String test) throws Exception {
		@SuppressWarnings("unchecked")
		ItemStreamReader<String> reader = (ItemStreamReader<String>) applicationContext.getBean(name);
		reader.open(new ExecutionContext());
		StopWatch stopWatch = new StopWatch(test);
		stopWatch.start();
		int count = 0;
		while (reader.read() != null) {
			// do nothing
			count++;
		}
		stopWatch.stop();
		reader.close();
		logger.info(stopWatch.shortSummary());
		return count;
	}

}
