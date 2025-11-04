/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.core.step.skip;

import java.util.HashSet;
import java.util.Set;

import org.springframework.util.Assert;

/**
 * A composite {@link SkipPolicy} that checks if the exception is assignable from one of
 * the given skippable exceptions, and counts the number of skips to not exceed a given
 * limit.
 *
 * @author Mahmoud Ben Hassine
 * @since 6.0
 */
public class LimitCheckingExceptionHierarchySkipPolicy implements SkipPolicy {

	private Set<Class<? extends Throwable>> skippableExceptions = new HashSet<>();

	private final long skipLimit;

	/**
	 * Create a new {@link LimitCheckingExceptionHierarchySkipPolicy} instance.
	 * @param skippableExceptions exception classes that can be skipped (non-critical)
	 * @param skipLimit the number of skippable exceptions that are allowed to be skipped
	 */
	public LimitCheckingExceptionHierarchySkipPolicy(Set<Class<? extends Throwable>> skippableExceptions,
			long skipLimit) {
		Assert.notNull(skippableExceptions, "The skippableExceptions must not be null");
		Assert.isTrue(skipLimit > 0, "The skipLimit must be greater than zero");
		this.skippableExceptions = skippableExceptions;
		this.skipLimit = skipLimit;
	}

	@Override
	public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {
		if (!isSkippable(t)) {
			return false;
		}
		if (skipCount < this.skipLimit) {
			return true;
		}
		else {
			throw new SkipLimitExceededException(this.skipLimit, t);
		}
	}

	private boolean isSkippable(Throwable t) {
		boolean isSkippable = false;
		for (Class<? extends Throwable> skippableException : this.skippableExceptions) {
			if (skippableException.isAssignableFrom(t.getClass())) {
				isSkippable = true;
				break;
			}
		}
		return isSkippable;
	}

}