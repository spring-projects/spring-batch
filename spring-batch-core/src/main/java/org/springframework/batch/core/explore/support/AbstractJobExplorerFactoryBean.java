package org.springframework.batch.core.explore.support;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} that automates the creation of a
 * {@link SimpleJobExplorer}. Declares abstract methods for providing DAO
 * object implementations.
 * 
 * @see JobExplorerFactoryBean
 * @see MapJobExplorerFactoryBean
 * 
 * @author Dave Syer
 */
public abstract class AbstractJobExplorerFactoryBean implements FactoryBean {

	/**
	 * @return fully configured {@link JobInstanceDao} implementation.
	 */
	protected abstract JobInstanceDao createJobInstanceDao() throws Exception;

	/**
	 * @return fully configured {@link JobExecutionDao} implementation.
	 */
	protected abstract JobExecutionDao createJobExecutionDao() throws Exception;
	
	protected abstract StepExecutionDao createStepExecutionDao() throws Exception;

	/**
	 * The type of object to be returned from {@link #getObject()}.
	 * 
	 * @return JobExplorer.class
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	public Class<JobExplorer> getObjectType() {
		return JobExplorer.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
