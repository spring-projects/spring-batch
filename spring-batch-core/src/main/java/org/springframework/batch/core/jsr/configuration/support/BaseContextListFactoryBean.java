/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.FactoryBean;

/**
 * A simple factory bean that consolidates the list of locations to look for the base context for the JSR-352
 * functionality
 *
 * @author Michael Minella
 * @since 3.0.3
 */
public class BaseContextListFactoryBean implements FactoryBean<List<String>>{

	@Override
	public List<String> getObject() throws Exception {
		String overrideContextLocation = System.getProperty("JSR-352-BASE-CONTEXT");

		List<String> contextLocations = new ArrayList<>(2);

		contextLocations.add("baseContext.xml");

		if(overrideContextLocation != null) {
			contextLocations.add(overrideContextLocation);
		}

		return contextLocations;
	}

	@Override
	public Class<?> getObjectType() {
		return List.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
