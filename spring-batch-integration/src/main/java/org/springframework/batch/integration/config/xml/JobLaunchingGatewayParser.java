/*
   * Copyright 2002-2013 the original author or authors.
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
package org.springframework.batch.integration.config.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.integration.launch.JobLaunchingGateway;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * The parser for the Job-Launching Gateway, which will instantiate a
 * {@link JobLaunchingGatewayParser}. If no {@link JobLauncher} reference has
 * been provided, this parse will use the use the globally registered bean
 * 'jobLauncher'.
 *
 * @author Gunnar Hillert
 * @since 1.3
 *
 */
public class JobLaunchingGatewayParser extends AbstractConsumerEndpointParser {

	private static final Log logger = LogFactory.getLog(JobLaunchingGatewayParser.class);

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {

		final BeanDefinitionBuilder jobLaunchingGatewayBuilder =
			BeanDefinitionBuilder.genericBeanDefinition(JobLaunchingGateway.class);

		final String jobLauncher = element.getAttribute("job-launcher");

		if (StringUtils.hasText(jobLauncher)) {
			jobLaunchingGatewayBuilder.addConstructorArgReference(jobLauncher);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("No jobLauncher specified, using default 'jobLauncher' reference instead.");
			}
			jobLaunchingGatewayBuilder.addConstructorArgReference("jobLauncher");
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(jobLaunchingGatewayBuilder, element, "reply-timeout", "sendTimeout");

		final String replyChannel = element.getAttribute("reply-channel");

		if (StringUtils.hasText(replyChannel)) {
			jobLaunchingGatewayBuilder.addPropertyReference("outputChannel", replyChannel);
		}

		return jobLaunchingGatewayBuilder;

	}

}
