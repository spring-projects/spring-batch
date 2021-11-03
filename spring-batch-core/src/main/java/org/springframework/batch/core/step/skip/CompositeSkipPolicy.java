/*
 * Copyright 2006-2021 the original author or authors.
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

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class CompositeSkipPolicy implements SkipPolicy {

	private SkipPolicy[] skipPolicies;

	/**
	 * Default Constructor that establishes {@link SkipPolicy}s as an empty array.
	 */
	public CompositeSkipPolicy() {
		this(new SkipPolicy[0]);
	}

	/**
	 * Constructor that establishes the {@link SkipPolicy}s for the {@link CompositeSkipPolicy}.
	 * @param skipPolicies array containing {@link SkipPolicy}s.
	 */
	public CompositeSkipPolicy(SkipPolicy[] skipPolicies) {
		this.skipPolicies = skipPolicies;
	}

	/**
	 * Establish the {@link SkipPolicy}s for the {@link CompositeSkipPolicy}.
	 * @param skipPolicies array containing {@link SkipPolicy}s.
	 */
	public void setSkipPolicies(SkipPolicy[] skipPolicies) {
		this.skipPolicies = skipPolicies;
	}

	@Override
	public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {
		for (SkipPolicy policy : skipPolicies) {
			if (policy.shouldSkip(t, skipCount)) {
				return true;
			}
		}
		return false;
	}

}
