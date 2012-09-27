package org.springframework.batch.core.scope.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.util.ReflectionUtils;

/**
 * JobSynchronizationManagerTests.
 * 
 * @author Jimmy Praet
 */
public class JobSynchronizationManagerTests {

	private JobExecution jobExecution = new JobExecution(0L);

	@Before
	@After
	public void start() {
		while (JobSynchronizationManager.getContext() != null) {
			JobSynchronizationManager.close();
		}
	}

	@Test
	public void testGetContext() {
		assertNull(JobSynchronizationManager.getContext());
		JobSynchronizationManager.register(jobExecution);
		assertNotNull(JobSynchronizationManager.getContext());
	}

	@Test
	public void testClose() throws Exception {
		final List<String> list = new ArrayList<String>();
		JobContext context = JobSynchronizationManager.register(jobExecution);
		context.registerDestructionCallback("foo", new Runnable() {
			public void run() {
				list.add("foo");
			}
		});
		JobSynchronizationManager.close();
		assertNull(JobSynchronizationManager.getContext());
		assertEquals(0, list.size());
		// check for possible memory leak
		assertEquals(0, extractStaticMap("counts").size());
		assertEquals(0, extractStaticMap("contexts").size());
	}

	private Map<?, ?> extractStaticMap(String name) throws IllegalAccessException {
		Field field = ReflectionUtils.findField(JobSynchronizationManager.class, "synchronizationManager");
		ReflectionUtils.makeAccessible(field);
		SynchronizationManagerSupport<?, ?> synchronizationManager =
				(SynchronizationManagerSupport<?, ?>) field.get(JobSynchronizationManager.class);
		field = ReflectionUtils.findField(SynchronizationManagerSupport.class, name);
		ReflectionUtils.makeAccessible(field);
		Map<?, ?> map = (Map<?, ?>) field.get(synchronizationManager);
		return map;
	}
	@Test
	public void testMultithreaded() throws Exception {
		JobContext context = JobSynchronizationManager.register(jobExecution);
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		FutureTask<JobContext> task = new FutureTask<JobContext>(new Callable<JobContext>() {
			public JobContext call() throws Exception {
				try {
					JobSynchronizationManager.register(jobExecution);
					JobContext context = JobSynchronizationManager.getContext();
					context.setAttribute("foo", "bar");
					return context;
				}
				finally {
					JobSynchronizationManager.close();
				}
			}
		});
		executorService.execute(task);
		executorService.awaitTermination(1, TimeUnit.SECONDS);
		assertEquals(context.attributeNames().length, task.get().attributeNames().length);
		JobSynchronizationManager.close();
		assertNull(JobSynchronizationManager.getContext());
	}

	@Test
	public void testRelease() {
		JobContext context = JobSynchronizationManager.register(jobExecution);
		final List<String> list = new ArrayList<String>();
		context.registerDestructionCallback("foo", new Runnable() {
			public void run() {
				list.add("foo");
			}
		});
		// On release we expect the destruction callbacks to be called
		JobSynchronizationManager.release();
		assertNull(JobSynchronizationManager.getContext());
		assertEquals(1, list.size());
	}

	@Test
	public void testRegisterNull() {
		assertNull(JobSynchronizationManager.getContext());
		JobSynchronizationManager.register(null);
		assertNull(JobSynchronizationManager.getContext());
	}

	@Test
	public void testRegisterTwice() {
		JobSynchronizationManager.register(jobExecution);
		JobSynchronizationManager.register(jobExecution);
		JobSynchronizationManager.close();
		// if someone registers you have to assume they are going to close, so
		// the last thing you want is for the close to remove another context
		// that someone else has registered
		assertNotNull(JobSynchronizationManager.getContext());
		JobSynchronizationManager.close();
		assertNull(JobSynchronizationManager.getContext());
	}

}