/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.step.skip;

/**
 * Policy for determining whether or not some processing should be skipped.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
public interface SkipPolicy {

	/**
	 * Returns true or false, indicating whether or not processing should
	 * continue with the given throwable. Clients may use
	 * <code>skipCount<0</code> to probe for exception types that are skippable,
	 * so implementations should be able to handle gracefully the case where
	 * <code>skipCount<0</code>. Implementations should avoid throwing any
	 * undeclared exceptions.
	 * 
	 * @param t exception encountered while reading
	 * @param skipCount currently running count of skips
	 * @return true if processing should continue, false otherwise.
	 * @throws SkipLimitExceededException if a limit is breached
	 * @throws IllegalArgumentException if the exception is null
	 */
	boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException;

}
