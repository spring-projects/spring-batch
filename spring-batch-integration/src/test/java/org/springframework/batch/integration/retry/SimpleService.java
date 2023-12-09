/*
 * Copyright 2008-2023 the original author or authors.
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
package org.springframework.batch.integration.retry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;

@MessageEndpoint
public class SimpleService implements Service {

	private final Log logger = LogFactory.getLog(getClass());

	private final List<String> processed = new CopyOnWriteArrayList<>();

	private List<String> expected = new ArrayList<>();

	private final AtomicInteger count = new AtomicInteger(0);

	public void setExpected(List<String> expected) {
		this.expected = expected;
	}

	/**
	 * Public getter for the processed.
	 * @return the processed
	 */
	public List<String> getProcessed() {
		return processed;
	}

	@Override
	@ServiceActivator(inputChannel = "requests", outputChannel = "replies")
	public String process(String message) {
		String result = message + ": " + count.incrementAndGet();
		logger.debug("Handling: " + message);
		if (count.get() <= expected.size()) {
			processed.add(message);
		}
		if ("fail".equals(message)) {
			throw new RuntimeException("Planned failure");
		}
		return result;
	}

}
