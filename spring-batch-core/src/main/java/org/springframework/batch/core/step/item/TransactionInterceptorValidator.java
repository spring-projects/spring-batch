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
package org.springframework.batch.core.step.item;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.batch.item.validator.Validator;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;

/**
 * Simple validator for internal use only (package private to make it testable).
 * Asserts that its argument has no more than the specified number of
 * transaction interceptors in its advice chain.
 * 
 * @author Dave Syer
 * 
 */
class TransactionInterceptorValidator implements Validator {

	protected Log logger = LogFactory.getLog(getClass());

	private final int maxCount;

	/**
	 * @param maxCount
	 */
	public TransactionInterceptorValidator(int maxCount) {
		super();
		this.maxCount = maxCount;
	}

	/**
	 * Assert that the object passed in has no more than the maximum number of
	 * transaction interceptors in its advice chain.
	 * 
	 * @see org.springframework.batch.item.validator.Validator#validate(java.lang.Object)
	 */
	public void validate(Object value) throws ValidationException {
		Assert.notNull(value, "JobRepository must be provided");
		Assert.state(countTransactionInterceptors(value) <= maxCount,
				"JobRepository has more than one transaction interceptor.  "
						+ "Do not declare a separate transaction advice if using the JobRepositoryFactoryBean.");
	}

	/**
	 * @param object an Object, possibly advised
	 * @return the number of transaction interceptors in the advice chain
	 */
	private int countTransactionInterceptors(Object object) {
		int count = 0;
		Object target = object;
		while (target instanceof Advised) {
			Advised advised = (Advised) target;
			Advisor[] interceptors = advised.getAdvisors();
			for (int i = 0; i < interceptors.length; i++) {
				if (interceptors[i].getAdvice() instanceof TransactionInterceptor) {
					count++;
				}
			}
			try {
				target = advised.getTargetSource().getTarget();
			}
			catch (Exception e) {
				logger.warn("Target could not be obtained from advised instance.", e);
			}
		}
		return count;
	}

}
