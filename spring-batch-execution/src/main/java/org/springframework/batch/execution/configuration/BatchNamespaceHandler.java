/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.batch.execution.configuration;

import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * <code>NamespaceHandler</code> for the <code>batch-execution</code> namespace.
 * 
 * <p>
 * Provides a {@link BeanDefinitionParser} for the <code>&lt;batch:job&gt;</code> tag. A <code>job</code> tag can
 * include nested <code>chunked-step</code> and <code>tasklet-step</code> tags.
 * 
 * <p>
 * Provides a {@link BeanDefinitionParser} for the <code>&lt;batch:job-repository&gt;</code> tag.
 * 
 * @author Ben Hale
 */
public class BatchNamespaceHandler extends NamespaceHandlerSupport {

	/**
	 * Register the {@link BeanDefinitionParser BeanDefinitionParser} for the '<code>job</code>', tag.
	 */
	public void init() {
		registerBeanDefinitionParser("job", new JobBeanDefinitionParser());
		registerBeanDefinitionParser("job-repository", new JobRepositoryBeanDefinitionParser());
	}

}
