/*
 * Copyright 2024-2025 the original author or authors.
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
package org.springframework.batch.core.repository.dao.mongodb;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mahmoud Ben Hassine
 * @author Christoph Strobl
 * @since 5.2.0
 */
public class MongoSequenceIncrementer implements DataFieldMaxValueIncrementer {

	private static final int NODE_BITS = 10;
	private static final int SEQUENCE_BITS = 12;
	private static final int NODE_SHIFT = SEQUENCE_BITS;
	private static final int TIMESTAMP_SHIFT = NODE_BITS + SEQUENCE_BITS;
	private static final int SEQUENCE_MASK = (1 << SEQUENCE_BITS) - 1;
	private static final int NODE_MASK = (1 << NODE_BITS) - 1;
	
	private static final long TSID_EPOCH = 1577836800000L;
	
	private final int nodeId;
	private final AtomicInteger sequence = new AtomicInteger(0);
	private volatile long lastTimestamp = -1L;

    private static final SecureRandom random = new SecureRandom();

	public MongoSequenceIncrementer() {
		this.nodeId = calculateNodeId();
	}

	public MongoSequenceIncrementer(int nodeId) {
		if (nodeId < 0 || nodeId > NODE_MASK) {
			throw new IllegalArgumentException("Node ID must be between 0 and " + NODE_MASK);
		}
		this.nodeId = nodeId;
	}

	@Override
	public long nextLongValue() throws DataAccessException {
		return generateTsid();
	}

	@Override
	public int nextIntValue() throws DataAccessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String nextStringValue() throws DataAccessException {
		throw new UnsupportedOperationException();
	}

	private synchronized long generateTsid() {
		long timestamp = System.currentTimeMillis() - TSID_EPOCH;
		
		if (timestamp < lastTimestamp) {
			timestamp = lastTimestamp;
		}
		
		if (timestamp == lastTimestamp) {
			int seq = sequence.incrementAndGet() & SEQUENCE_MASK;
			if (seq == 0) {
				timestamp = waitNextMillis(lastTimestamp);
				lastTimestamp = timestamp;
			}
			return (timestamp << TIMESTAMP_SHIFT) | ((long) nodeId << NODE_SHIFT) | seq;
		} else {
			sequence.set(0);
			lastTimestamp = timestamp;
			return (timestamp << TIMESTAMP_SHIFT) | ((long) nodeId << NODE_SHIFT);
		}
	}

	private long waitNextMillis(long lastTimestamp) {
		long timestamp = System.currentTimeMillis() - TSID_EPOCH;
		while (timestamp <= lastTimestamp) {
			timestamp = System.currentTimeMillis() - TSID_EPOCH;
		}
		return timestamp;
	}

	private int calculateNodeId() {
		try {
			String hostname = InetAddress.getLocalHost().getHostName();
			int hostHash = hostname.hashCode();
            long processId = ProcessHandle.current().pid();
			long randomValue = random.nextInt();
			return (int) ((hostHash ^ processId ^ randomValue) & NODE_MASK);
		} catch (Exception e) {
			return (int) ((System.nanoTime() ^ Thread.currentThread().getId()) & NODE_MASK);
		}
	}

}
