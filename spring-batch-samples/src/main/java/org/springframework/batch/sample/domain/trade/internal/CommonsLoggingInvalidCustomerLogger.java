/*
 * Copyright 2006-2014 the original author or authors.
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

package org.springframework.batch.sample.domain.trade.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.launch.support.CommandLineJobRunner;
import org.springframework.batch.sample.domain.trade.CustomerUpdate;
import org.springframework.batch.sample.domain.trade.InvalidCustomerLogger;

/**
 * @author Lucas Ward
 *
 */
public class CommonsLoggingInvalidCustomerLogger implements InvalidCustomerLogger {

	protected static final Log LOG = LogFactory.getLog(CommandLineJobRunner.class);

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.sample.domain.trade.InvalidCustomerLogger#log(org.
	 * springframework.batch.sample.domain.trade.CustomerUpdate)
	 */
	@Override
	public void log(CustomerUpdate customerUpdate) {
		LOG.error("invalid customer encountered: [ " + customerUpdate + "]");
	}

}
