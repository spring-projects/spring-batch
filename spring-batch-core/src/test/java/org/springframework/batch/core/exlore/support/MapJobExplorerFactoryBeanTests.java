package org.springframework.batch.core.exlore.support;

import org.junit.Test;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.MapJobExplorerFactoryBean;

/**
 * Tests for {@link MapJobExplorerFactoryBean}.
 */
public class MapJobExplorerFactoryBeanTests {

	private MapJobExplorerFactoryBean tested = new MapJobExplorerFactoryBean();

	/**
	 * Use the factory to create repository and check the repository remembers
	 * created executions.
	 */
	@Test
	public void testCreateRepository() throws Exception {
		JobExplorer explorer = (JobExplorer) tested.getObject();

		explorer.findRunningJobExecutions("foo");

	}

}
