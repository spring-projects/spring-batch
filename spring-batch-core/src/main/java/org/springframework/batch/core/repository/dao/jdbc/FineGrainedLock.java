/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.core.repository.dao.jdbc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Fine-grained lock implementation.
 *
 * @author Yanming Zhou
 */
class FineGrainedLock<T> {

	record Wrapper(Lock lock, AtomicInteger numberOfThreadsInQueue) {

		private Wrapper addThreadInQueue() {
			numberOfThreadsInQueue.incrementAndGet();
			return this;
		}

		private int removeThreadFromQueue() {
			return numberOfThreadsInQueue.decrementAndGet();
		}
	}

	private final ConcurrentHashMap<T, Wrapper> locks = new ConcurrentHashMap<>();

	public void lock(T key) {
		Wrapper wrapper = locks.compute(key,
				(k, v) -> v == null ? new Wrapper(new ReentrantLock(), new AtomicInteger(1)) : v.addThreadInQueue());
		wrapper.lock.lock();
	}

	public void unlock(T key) {
		Wrapper wrapper = locks.get(key);
		if (wrapper == null) {
			throw new IllegalStateException("Lock on '" + key + "' doesn't exist, please lock it first");
		}
		wrapper.lock.unlock();
		if (wrapper.removeThreadFromQueue() == 0) {
			locks.remove(key, wrapper);
		}
	}

}
