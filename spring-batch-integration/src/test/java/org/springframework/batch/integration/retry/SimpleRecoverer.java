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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.retry.interceptor.MethodInvocationRecoverer;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public final class SimpleRecoverer implements MethodInvocationRecoverer<String> {

	private final Log logger = LogFactory.getLog(getClass());

	private final List<String> recovered = new ArrayList<>();

	/**
	 * Public getter for the recovered.
	 * @return the recovered
	 */
	public List<String> getRecovered() {
		return recovered;
	}

	@Override
	public @Nullable String recover(Object[] data, Throwable cause) {
		if (data == null) {
			return null;
		}
		String payload = (String) data[0];
		logger.debug("Recovering: " + payload);
		recovered.add(payload);
		return null;
	}

}