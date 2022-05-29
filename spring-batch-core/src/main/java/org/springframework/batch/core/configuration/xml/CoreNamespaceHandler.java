/*
 * Copyright 2006-2013 the original author or authors.
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

import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @author Dave Syer
 *
 */
public class CoreNamespaceHandler extends NamespaceHandlerSupport {

	/**
	 * @see NamespaceHandler#init()
	 */
	@Override
	public void init() {
		this.registerBeanDefinitionParser("job", new JobParser());
		this.registerBeanDefinitionParser("flow", new TopLevelFlowParser());
		this.registerBeanDefinitionParser("step", new TopLevelStepParser());
		this.registerBeanDefinitionParser("job-repository", new JobRepositoryParser());
		this.registerBeanDefinitionParser("job-listener", new TopLevelJobListenerParser());
		this.registerBeanDefinitionParser("step-listener", new TopLevelStepListenerParser());
	}

}
