/*
 * Copyright 2006-2023 the original author or authors.
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

import org.springframework.batch.core.step.Step;

/**
 * Exception indicating that the skip limit for a particular {@link Step} has been
 * exceeded.
 *
 * @author Ben Hale
 * @author Lucas Ward
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 */
public class SkipLimitExceededException extends SkipException {

	private final long skipLimit;

	public SkipLimitExceededException(long skipLimit, Throwable t) {
		super("Skip limit of '" + skipLimit + "' exceeded", t);
		this.skipLimit = skipLimit;
	}

	public long getSkipLimit() {
		return skipLimit;
	}

}
