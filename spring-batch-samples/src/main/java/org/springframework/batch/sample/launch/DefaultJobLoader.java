/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.sample.launch;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.ListableJobRegistry;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Assert;

public class DefaultJobLoader implements JobLoader, ApplicationContextAware {

	private ListableJobRegistry registry;

	private ApplicationContext applicationContext;

	private Map<String, String> configurations = new HashMap<String, String>();

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void setRegistry(ListableJobRegistry registry) {
		this.registry = registry;
	}

	public Map<String, String> getConfigurations() {
		Map<String, String> result = new HashMap<String, String>(configurations);
		for (String jobName : registry.getJobNames()) {
			try {
				Job configuration = registry.getJob(jobName);
				String name = configuration.getName();
				if (!configurations.containsKey(name)) {
					result.put(name, "<unknown path>: " + configuration);
				}
			}
			catch (NoSuchJobException e) {
				throw new IllegalStateException("Registry could not locate its own job (NoSuchJobException).");
			}
		}
		return result;
	}

	public void loadResource(String path) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] { path },
				applicationContext);
		String[] names = context.getBeanNamesForType(Job.class);
		for (String name : names) {
			configurations.put(name, path);
		}
	}

	public Object getJobConfiguration(String name) {
		try {
			return registry.getJob(name);
		}
		catch (NoSuchJobException e) {
			return null;
		}
	}

	public Object getProperty(String path) {
		int index = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(path);
		BeanWrapperImpl wrapper = createBeanWrapper(path, index);
		String key = path.substring(index + 1);
		return wrapper.getPropertyValue(key);
	}

	public void setProperty(String path, String value) {
		int index = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(path);
		BeanWrapperImpl wrapper = createBeanWrapper(path, index);
		String key = path.substring(index + 1);
		wrapper.setPropertyValue(key, value);
	}

	private BeanWrapperImpl createBeanWrapper(String path, int index) {
		Assert.state(index > 0, "Path must be nested, e.g. bean.value");
		String name = path.substring(0, index);
		Object bean = getJobConfiguration(name);
		Assert.notNull(bean, "No JobConfiguration exists with name=" + name);
		return new BeanWrapperImpl(bean);
	}

}
