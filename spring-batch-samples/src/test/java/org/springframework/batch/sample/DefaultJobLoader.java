/**
 * 
 */
package org.springframework.batch.sample;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.repository.ListableJobRegistry;
import org.springframework.batch.core.repository.NoSuchJobException;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Assert;

public class DefaultJobLoader implements JobLoader,
		ApplicationContextAware {

	private ListableJobRegistry registry;
	private ApplicationContext applicationContext;
	private Map configurations = new HashMap();

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void setRegistry(ListableJobRegistry registry) {
		this.registry = registry;
	}

	public Map getConfigurations() {
		Map result = new HashMap(configurations);
		for (Iterator iterator = registry.getJobNames().iterator(); iterator
				.hasNext();) {
			try {
				Job configuration = (Job) registry.getJob((String) iterator.next());
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
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { path }, applicationContext);
		String[] names = context.getBeanNamesForType(Job.class);
		for (int i = 0; i < names.length; i++) {
			String name = names[i];
			configurations.put(name, path);
		}
	}
	
	public Object getJobConfiguration(String name) {
		try {
			return registry.getJob(name);
		} catch (NoSuchJobException e) {
			return null;
		}
	}

	public Object getProperty(String path) {
		int index = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(path);
		BeanWrapperImpl wrapper = createBeanWrapper(path, index);
		String key = path.substring(index+1);
		return wrapper.getPropertyValue(key);
	}

	public void setProperty(String path, String value) {
		int index = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(path);
		BeanWrapperImpl wrapper = createBeanWrapper(path, index);
		String key = path.substring(index+1);
		wrapper.setPropertyValue(key, value);
	}

	private BeanWrapperImpl createBeanWrapper(String path, int index) {
		Assert.state(index>0, "Path must be nested, e.g. bean.value");
		String name = path.substring(0,index);
		Object bean = getJobConfiguration(name);
		Assert.notNull(bean, "No JobConfiguration exists with name="+name);
		BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);
		return wrapper;
	}

}
