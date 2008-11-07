package org.springframework.batch.sample.tasklet;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Deletes files from an array of resources, so a pattern can be used in
 * configuration, e.g. <code>resources="/home/batch/job/**"</code>
 * 
 * @author Robert Kasanicky
 */
public class FileDeletingTasklet implements Tasklet, InitializingBean {

	private Resource[] resources;

	public ExitStatus execute(StepContribution contribution, AttributeAccessor attributes) throws Exception {
		for (Resource resource : resources) {
			boolean deleted = resource.getFile().delete();
			if (!deleted) {
				throw new UnexpectedJobExecutionException("Could not delete file " + resource);
			}
		}
		return ExitStatus.FINISHED;
	}

	public void setResources(Resource[] resources) {
		this.resources = resources;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(resources, "Resources must be set");
	}

}
