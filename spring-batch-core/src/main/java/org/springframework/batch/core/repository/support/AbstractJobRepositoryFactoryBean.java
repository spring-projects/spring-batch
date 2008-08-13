package org.springframework.batch.core.repository.support;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} that automates the creation of a
 * {@link SimpleJobRepository}. Declares abstract methods for providing DAO
 * object implementations.
 * 
 * @see JobRepositoryFactoryBean
 * @see MapJobRepositoryFactoryBean
 * 
 * @author Ben Hale
 * @author Lucas Ward
 * @author Robert Kasanicky
 */
public abstract class AbstractJobRepositoryFactoryBean implements FactoryBean {

	/**
	 * @return fully configured {@link JobInstanceDao} implementation.
	 */
	protected abstract JobInstanceDao createJobInstanceDao() throws Exception;

	/**
	 * @return fully configured {@link JobExecutionDao} implementation.
	 */
	protected abstract JobExecutionDao createJobExecutionDao() throws Exception;

	/**
	 * @return fully configured {@link StepExecutionDao} implementation.
	 */
	protected abstract StepExecutionDao createStepExecutionDao() throws Exception;
	
	/**
	 * @return fully configured {@link ExecutionContextDao} implementation.
	 */
	protected abstract ExecutionContextDao createExecutionContextDao() throws Exception;

	/**
	 * The type of object to be returned from {@link #getObject()}.
	 * 
	 * @return JobRepository.class
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	public Class<JobRepository> getObjectType() {
		return JobRepository.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
