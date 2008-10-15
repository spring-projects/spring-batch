package org.springframework.batch.core.repository.support;

import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.MapExecutionContextDao;
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

	/**
	 * Convenience method to clear all the map daos globally, removing all
	 * entities.
	 */
	public static void clear() {
		MapJobInstanceDao.clear();
		MapJobExecutionDao.clear();
		MapStepExecutionDao.clear();
		MapExecutionContextDao.clear();
	}

	@Override
	protected JobExecutionDao createJobExecutionDao() throws Exception {
		return new MapJobExecutionDao();
	}

	@Override
	protected JobInstanceDao createJobInstanceDao() throws Exception {
		return new MapJobInstanceDao();
	}

	@Override
	protected StepExecutionDao createStepExecutionDao() throws Exception {
		return new MapStepExecutionDao();
	}

	@Override
	protected ExecutionContextDao createExecutionContextDao() throws Exception {
		return new MapExecutionContextDao();
	}

}
