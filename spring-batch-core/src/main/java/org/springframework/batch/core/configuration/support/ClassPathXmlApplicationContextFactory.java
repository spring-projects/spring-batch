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

package org.springframework.batch.core.configuration.support;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * {@link ApplicationContextFactory} implementation that takes a parent context
 * and a path to the context to create. When createApplicationContext method is
 * called, the child {@link ApplicationContext} will be returned. The child
 * context is not re-created every time it is requested, it is lazily
 * initialized and cached. Clients should ensure that it is closed when it is no
 * longer needed. If a path is not set, the parent will always be returned.
 * 
 * @deprecated use {@link GenericApplicationContextFactory} instead
 */
@Deprecated
public class ClassPathXmlApplicationContextFactory extends GenericApplicationContextFactory {

	/**
	 * Create an application context factory for the resource specified.
	 * 
	 * @param resource a resource (XML configuration file)
	 */
	public ClassPathXmlApplicationContextFactory(Resource resource) {
		super(resource);
	}

}
