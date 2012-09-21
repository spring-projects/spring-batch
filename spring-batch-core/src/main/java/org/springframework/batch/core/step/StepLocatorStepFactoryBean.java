package org.springframework.batch.core.step;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.beans.factory.FactoryBean;

/**
 * Convenience factory for {@link Step} instances given a {@link StepLocator}.
 * Most implementations of {@link Job} implement StepLocator, so that can be a
 * good starting point.
 * 
 * @author Dave Syer
 * 
 */
public class StepLocatorStepFactoryBean implements FactoryBean<Step> {

	public StepLocator stepLocator;

	public String stepName;

	/**
	 * @param stepLocator
	 */
	public void setStepLocator(StepLocator stepLocator) {
		this.stepLocator = stepLocator;
	}

	/**
	 * @param stepName
	 */
	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

	/**
	 * 
	 * @see FactoryBean#getObject()
	 */
	public Step getObject() throws Exception {
		return stepLocator.getStep(stepName);
	}

	/**
	 * Tell clients that we are a factory for {@link Step} instances.
	 * 
	 * @see FactoryBean#getObjectType()
	 */
	public Class<? extends Step> getObjectType() {
		return Step.class;
	}

	/**
	 * Always return true as optimization for bean factory.
	 * 
	 * @see FactoryBean#isSingleton()
	 */
	public boolean isSingleton() {
		return true;
	}

}
