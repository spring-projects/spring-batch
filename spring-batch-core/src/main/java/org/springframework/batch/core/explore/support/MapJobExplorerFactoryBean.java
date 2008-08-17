package org.springframework.batch.core.explore.support;

import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.MapJobExecutionDao;
import org.springframework.batch.core.repository.dao.MapJobInstanceDao;
import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} that automates the creation of a
 * {@link SimpleJobExplorer} using in-memory DAO implementations.
 * 
 * @author Dave Syer
 */
public class MapJobExplorerFactoryBean extends AbstractJobExplorerFactoryBean {

	@Override
	protected JobExecutionDao createJobExecutionDao() throws Exception {
		return new MapJobExecutionDao();
	}

	@Override
	protected JobInstanceDao createJobInstanceDao() throws Exception {
		return new MapJobInstanceDao();
	}

	public Object getObject() throws Exception {
		return new SimpleJobExplorer(createJobInstanceDao(), createJobExecutionDao());
	}

}
