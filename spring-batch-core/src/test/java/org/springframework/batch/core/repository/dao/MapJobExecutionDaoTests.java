/*
 * Copyright 2008-2014 the original author or authors.
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
package org.springframework.batch.core.repository.dao;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;

@RunWith(JUnit4.class)
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
		JobExecution jobExecution = new JobExecution(new JobInstance((long) 1, "mapJob"), new JobParameters());

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
		final SortedSet<Long> ids = Collections.synchronizedSortedSet(new TreeSet<>()); // TODO Change to SkipList w/JDK6
		final AtomicReference<Exception> exception = new AtomicReference<>(null);

		// Implementation of the high-concurrency code
		final Runnable codeUnderTest = new Runnable() {
			@Override
			public void run() {
				try {
					JobExecution jobExecution = new JobExecution(new JobInstance((long) -1, "mapJob"), new JobParameters());
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
		if(exception.get() != null) {
			throw new RuntimeException("Exception occurred under high concurrency usage", exception.get());
		}

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
