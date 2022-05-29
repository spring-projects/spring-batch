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

package org.springframework.batch.core.test.football.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.ExceptionHandler;

public class FootballExceptionHandler implements ExceptionHandler {

	private static final Log logger = LogFactory.getLog(FootballExceptionHandler.class);

	@Override
	public void handleException(RepeatContext context, Throwable throwable) throws Throwable {

		if (!(throwable instanceof NumberFormatException)) {
			throw throwable;
		}
		else {
			logger.error("Number Format Exception!", throwable);
		}

	}

}
