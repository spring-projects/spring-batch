package org.springframework.batch.core.repository.support;

import junit.framework.TestCase;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Tests for {@link MapJobRepositoryFactoryBean}.
 */
public class MapJobRepositoryFactoryBeanTests extends TestCase {

	private MapJobRepositoryFactoryBean tested = new MapJobRepositoryFactoryBean();

	private PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

	protected void setUp() throws Exception {
		tested.setTransactionManager(transactionManager);
		tested.afterPropertiesSet();
	}

	/**
	 * Use the factory to create repository and check the repository remembers
	 * created executions.
	 */
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
