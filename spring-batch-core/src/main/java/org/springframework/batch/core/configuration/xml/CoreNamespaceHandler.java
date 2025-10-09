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
package org.springframework.batch.core.configuration.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class CoreNamespaceHandler extends NamespaceHandlerSupport {

	private static final Log LOGGER = LogFactory.getLog(CoreNamespaceHandler.class);

	/**
	 * @see NamespaceHandler#init()
	 */
	@Override
	public void init() {
		LOGGER.info(
				"DEPRECATION NOTE: The batch XML namespace is deprecated as of Spring Batch 6.0 and will be removed in version 7.0.");
		this.registerBeanDefinitionParser("job", new JobParser());
		this.registerBeanDefinitionParser("flow", new TopLevelFlowParser());
		this.registerBeanDefinitionParser("step", new TopLevelStepParser());
		this.registerBeanDefinitionParser("job-repository", new JobRepositoryParser());
		this.registerBeanDefinitionParser("job-listener", new TopLevelJobListenerParser());
		this.registerBeanDefinitionParser("step-listener", new TopLevelStepListenerParser());
	}

}
