/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.core.launch.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @deprecated since 6.0 with no replacement, for removal in 6.2 or later.
 */
@Deprecated(since = "6.0", forRemoval = true)
public class RuntimeExceptionTranslator implements MethodInterceptor {

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		try {
			return invocation.proceed();
		}
		catch (Exception e) {
			if (e.getClass().getName().startsWith("java")) {
				throw e;
			}
			throw new RuntimeException(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

}
