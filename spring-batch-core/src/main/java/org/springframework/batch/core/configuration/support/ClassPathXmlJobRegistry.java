/**
 * 
 */
package org.springframework.batch.core.configuration.support;

import java.util.Collection;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.configuration.ListableJobRegistry;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

/**
 * Implementation of the {@link ListableJobRegistry} interface that assumes all Jobs will be loaded from
 * ClassPathXml resources.
 * 
 * @author Lucas Ward
 * @since 2.0
 */
public class ClassPathXmlJobRegistry implements ListableJobRegistry, ApplicationContextAware, InitializingBean {

	List<Resource> jobPaths;
	ApplicationContext parent;
	ListableJobRegistry jobRegistry;
	
	public ClassPathXmlJobRegistry(List<Resource> jobPaths) {
		this.jobPaths = jobPaths;
	}
	
	public Job getJob(String name) throws NoSuchJobException {
		return jobRegistry.getJob(name);
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		parent = applicationContext;
	}

	public void afterPropertiesSet() throws Exception {
		
		jobRegistry = new MapJobRegistry();
		for(Resource resource:jobPaths){
			ClassPathXmlApplicationContextFactory applicationContextFactory = new ClassPathXmlApplicationContextFactory();
			applicationContextFactory.setPath(new Resource[]{resource});
			applicationContextFactory.setApplicationContext(parent);
			ApplicationContext context = applicationContextFactory.createApplicationContext();
			String[] names = context.getBeanNamesForType(Job.class);
			
			if(names.length > 1){
				throw new DuplicateJobException("More than one Job found for resource: [" + resource + "]");
			}
			else if(names.length == 0){
				throw new NoSuchJobException("No Jobs found in resource: [" + resource + "]");
			}
			
			ApplicationContextJobFactory jobFactory = new ApplicationContextJobFactory(applicationContextFactory, names[0]);
			jobRegistry.register(jobFactory);
		}
	}

	public Collection<String> getJobNames() {
		return jobRegistry.getJobNames();
	}

	public void register(JobFactory jobFactory) throws DuplicateJobException {
		jobRegistry.register(jobFactory);
	}

	public void unregister(String jobName) {
		jobRegistry.unregister(jobName);
	}
}
