package org.springframework.batch.core.repository.support;

import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.MapJobExecutionDao;
import org.springframework.batch.core.repository.dao.MapJobInstanceDao;
import org.springframework.batch.core.repository.dao.MapStepExecutionDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} that automates the creation of a
 * {@link SimpleJobRepository} using non-persistent in-memory DAO
 * implementations.
 * 
 * @author Robert Kasanicky
 */
public class MapJobRepositoryFactoryBean extends AbstractJobRepositoryFactoryBean {

	protected JobExecutionDao createJobExecutionDao() throws Exception {
		return new MapJobExecutionDao();
	}

	protected JobInstanceDao createJobInstanceDao() throws Exception {
		return new MapJobInstanceDao();
	}

	protected StepExecutionDao createStepExecutionDao() throws Exception {
		return new MapStepExecutionDao();
	}

}
