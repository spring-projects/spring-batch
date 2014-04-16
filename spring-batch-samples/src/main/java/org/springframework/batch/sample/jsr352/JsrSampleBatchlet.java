/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.batch.sample.jsr352;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;

/**
 * <p>
 * Sample {@link javax.batch.api.Batchlet} implementation.
 * </p>
 *
 * @since 3.0
 * @author Chris Schaefer
 */
public class JsrSampleBatchlet extends AbstractBatchlet {
	private static final Log LOG = LogFactory.getLog(JsrSampleBatchlet.class);

	@Inject
	@BatchProperty
	private String remoteServiceURL;

	@Override
	public String process() throws Exception {
		LOG.info("Calling remote service at: " + remoteServiceURL);

		Thread.sleep(2000);

		LOG.info("Remote service call complete");

		return null;
	}
}
