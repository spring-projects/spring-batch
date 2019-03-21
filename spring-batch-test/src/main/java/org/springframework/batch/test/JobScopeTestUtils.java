/*
 * Copyright 2006-2010 the original author or authors.
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
package org.springframework.batch.test;

import java.util.concurrent.Callable;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.scope.JobScope;
import org.springframework.batch.core.scope.context.JobSynchronizationManager;

/**
 * Utility class for creating and manipulating {@link JobScope} in unit tests.
 * This is useful when you want to use the Spring test support and inject
 * dependencies into your test case that happen to be job scoped in the
 * application context.
 * 
 * @author Dave Syer
 * @author Jimmy Praet
 */
public class JobScopeTestUtils {

	public static <T> T doInJobScope(JobExecution jobExecution, Callable<T> callable) throws Exception {
		try {
			JobSynchronizationManager.register(jobExecution);
			return callable.call();
		}
		finally {
			JobSynchronizationManager.close();
		}
	}

}
