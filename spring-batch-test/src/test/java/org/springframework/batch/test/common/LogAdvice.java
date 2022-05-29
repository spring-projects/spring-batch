/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.test.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;

/**
 * Wraps calls for 'Processing' methods which output a single Object to write the string
 * representation of the object to the log.
 *
 * @author Lucas Ward
 */
public class LogAdvice {

	private static Log log = LogFactory.getLog(LogAdvice.class);

	/*
	 * Wraps original method and adds logging both before and after method
	 */
	public void doBasicLogging(JoinPoint pjp) throws Throwable {
		Object[] args = pjp.getArgs();
		StringBuilder output = new StringBuilder();

		output.append(pjp.getTarget().getClass().getName()).append(": ");
		output.append(pjp.toShortString()).append(": ");

		for (Object arg : args) {
			output.append(arg).append(" ");
		}

		log.info("Basic: " + output.toString());
	}

	public void doStronglyTypedLogging(Object item) {
		log.info("Processed: " + item);
	}

}
