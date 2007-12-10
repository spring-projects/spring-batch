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

package org.springframework.batch.retry.interceptor;

import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryInterceptor;
import org.springframework.batch.retry.RetryStatistics;

/**
 * A {@link RetryInterceptor} that counts the number of attempts, errors and
 * successful retry operations.
 * 
 * @author Dave Syer
 * 
 */
public class StatisticsRetryInterceptor extends RetryInterceptorSupport implements RetryStatistics {

	private int startedCount;

	private int completeCount;

	private int errorCount;

	private int abortCount;

	private String name;

	public synchronized int getAbortCount() {
		return abortCount;
	}

	public synchronized int getCompleteCount() {
		return completeCount;
	}

	public synchronized int getErrorCount() {
		return errorCount;
	}

	public synchronized int getStartedCount() {
		return startedCount;
	}

	public String getName() {
		return name == null ? this.toString() : name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public synchronized boolean open(RetryContext context, RetryCallback callback) {
		startedCount++;
		return super.open(context, callback);
	}

	public synchronized void onError(RetryContext context, RetryCallback callback, Throwable throwable) {
		errorCount++;
		super.onError(context, callback, throwable);
	}

	public synchronized void close(RetryContext context, RetryCallback callback, Throwable throwable) {
		if (throwable != null) {
			abortCount++;
		}
		else {
			completeCount++;
		}
		super.close(context, callback, throwable);
	}

}
