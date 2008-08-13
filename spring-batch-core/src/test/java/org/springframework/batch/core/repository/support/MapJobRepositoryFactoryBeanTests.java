package org.springframework.batch.core.repository.support;

import org.junit.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;

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
		JobRepository repository = (JobRepository) tested.getObject();
		Job job = new JobSupport("jobName");
		JobParameters jobParameters = new JobParameters();

		repository.createJobExecution(job, jobParameters);

		try {
			repository.createJobExecution(job, jobParameters);
		}
		catch (JobRestartException e) {
			// expected
		}
	}
}
