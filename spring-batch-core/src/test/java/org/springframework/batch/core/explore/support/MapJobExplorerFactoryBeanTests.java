package org.springframework.batch.core.explore.support;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.MapJobExplorerFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;

/**
 * Tests for {@link MapJobExplorerFactoryBean}.
 */
public class MapJobExplorerFactoryBeanTests {

	/**
	 * Use the factory to create repository and check the explorer remembers
	 * created executions.
	 */
	@Test
	public void testCreateExplorer() throws Exception {

		MapJobRepositoryFactoryBean repositoryFactory = new MapJobRepositoryFactoryBean();
		((JobRepository)repositoryFactory.getObject()).createJobExecution("foo", new JobParameters());
		
		MapJobExplorerFactoryBean tested = new MapJobExplorerFactoryBean(repositoryFactory);
		tested.afterPropertiesSet();

		JobExplorer explorer = (JobExplorer) tested.getObject();

		assertEquals(1, explorer.findRunningJobExecutions("foo").size());

	}

}
