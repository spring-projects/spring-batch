/*
 * Copyright 2006-2010 the original author or authors.
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
package org.springframework.batch.test;

import java.util.concurrent.Callable;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.StepScope;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;

/**
 * Utility class for creating and manipulating {@link StepScope} in unit tests.
 * This is useful when you want to use the Spring test support and inject
 * dependencies into your test case that happen to be step scoped in the
 * application context.
 * 
 * @author Dave Syer
 * 
 */
public class StepScopeTestUtils {

	public static <T> T doInStepScope(StepExecution stepExecution, Callable<T> callable) throws Exception {
		try {
			StepSynchronizationManager.register(stepExecution);
			return callable.call();
		}
		finally {
			StepSynchronizationManager.close();
		}
	}

}
