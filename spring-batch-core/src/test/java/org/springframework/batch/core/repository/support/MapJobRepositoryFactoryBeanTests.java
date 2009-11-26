package org.springframework.batch.core.repository.support;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;

/**
 * Tests for {@link MapJobRepositoryFactoryBean}.
 */
public class MapJobRepositoryFactoryBeanTests {

	private MapJobRepositoryFactoryBean tested = new MapJobRepositoryFactoryBean();

	/**
	 * Use the factory to create repository and check the repository remembers
	 * created executions.
	 */
	@Test
	public void testCreateRepository() throws Exception {
		tested.afterPropertiesSet();
		JobRepository repository = (JobRepository) tested.getObject();
		Job job = new JobSupport("jobName");
		JobParameters jobParameters = new JobParameters();

		repository.createJobExecution(job.getName(), jobParameters);

		try {
			repository.createJobExecution(job.getName(), jobParameters);
			fail("Expected JobExecutionAlreadyRunningException");
		}
		catch (JobExecutionAlreadyRunningException e) {
			// expected
		}
	}
}
