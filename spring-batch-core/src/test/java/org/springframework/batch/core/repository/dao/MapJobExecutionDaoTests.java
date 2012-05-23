package org.springframework.batch.core.repository.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;

import static org.junit.Assert.*;

@RunWith(JUnit4ClassRunner.class)
public class MapJobExecutionDaoTests extends AbstractJobExecutionDaoTests {

	@Override
	protected JobExecutionDao getJobExecutionDao() {
		return new MapJobExecutionDao();
	}
	
	@Override
	protected JobInstanceDao getJobInstanceDao() {
		return new MapJobInstanceDao();
	}

	/**
	 * Modifications to saved entity do not affect the persisted object.
	 */
	@Test
	public void testPersistentCopy() {
		JobExecutionDao tested = new MapJobExecutionDao();
		JobExecution jobExecution = new JobExecution(new JobInstance((long) 1, new JobParameters(), "mapJob"));
		
		assertNull(jobExecution.getStartTime());
		tested.saveJobExecution(jobExecution);
		jobExecution.setStartTime(new Date());
		
		JobExecution retrieved = tested.getJobExecution(jobExecution.getId());
		assertNull(retrieved.getStartTime());
		
		tested.updateJobExecution(jobExecution);
		jobExecution.setEndTime(new Date());
		assertNull(retrieved.getEndTime());
		
	}

	/**
	* Verify that the ids are properly generated even under heavy concurrent load
	*/
	@Test
	public void testConcurrentSaveJobExecution() throws Exception {
		final int iterations = 100;

		// Object under test
		final JobExecutionDao tested = new MapJobExecutionDao();

		// Support objects for this testing
		final CountDownLatch latch = new CountDownLatch(1);
		final SortedSet<Long> ids = Collections.synchronizedSortedSet(new TreeSet<Long>()); // TODO Change to SkipList w/JDK6
		final AtomicReference<Exception> exception = new AtomicReference<Exception>(null);

		// Implementation of the high-concurrency code
		final Runnable codeUnderTest = new Runnable() {
			public void run() {
				try {
					JobExecution jobExecution = new JobExecution(new JobInstance((long) -1, new JobParameters(), "mapJob"));
					latch.await();
					tested.saveJobExecution(jobExecution);
					ids.add(jobExecution.getId());
				} catch(Exception e) {
					exception.set(e);
				}
			}
		};

		// Create the threads
		final Thread[] threads = new Thread[iterations];
		for(int i = 0; i < iterations; i++) {
			Thread t = new Thread(codeUnderTest, "Map Job Thread #" + (i+1));
			t.setPriority(Thread.MAX_PRIORITY);
			t.setDaemon(true);
			t.start();
			Thread.yield();
			threads[i] = t;
		}

		// Let the high concurrency abuse begin!
		do { latch.countDown(); } while(latch.getCount() > 0);
		for(Thread t : threads) { t.join(); }

		// Ensure no general exceptions arose
		if(exception.get() != null) throw new RuntimeException("Excepion occurred under high concurrency usage", exception.get());

		// Validate the ids: we'd expect one of these three things to fail
		if(ids.size() < iterations) {
			fail("Duplicate id generated during high concurrency usage");	
		}
		if(ids.first() < 0) {
			fail("Generated an id less than zero during high concurrency usage: " + ids.first());
		}
		if(ids.last() > iterations) {
			fail("Generated an id larger than expected during high concurrency usage: " + ids.last());
		}
	}

}
