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

package org.springframework.batch.retry;

/**
 * Interface for statistics reporting of retry attempts. Counts the number of
 * retry attempts, successes, errors (including retries), and aborts.
 * 
 * @author Dave Syer
 * 
 */
public interface RetryStatistics {

	/**
	 * @return the number of completed retry attempts (successful or not).
	 */
	int getCompleteCount();

	/**
	 * Get the number of times a retry block has been entered, irrespective of
	 * how many times the operation was retried.
	 * 
	 * @return the number of retry blocks started.
	 */
	int getStartedCount();

	/**
	 * Get the number of errors detected, whether or not they resulted in a
	 * retry.
	 * 
	 * @return the number of errors detected.
	 */
	int getErrorCount();

	/**
	 * Get the number of times a block failed to complete successfully, even
	 * after retry.
	 * 
	 * @return the number of retry attempts that failed overall.
	 */
	int getAbortCount();

	/**
	 * Get an identifier for the retry block for reporting purposes.
	 * 
	 * @return an identifier for the block.
	 */
	String getName();

}
