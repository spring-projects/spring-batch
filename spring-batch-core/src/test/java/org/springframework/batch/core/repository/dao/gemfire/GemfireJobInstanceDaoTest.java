package org.springframework.batch.core.repository.dao.gemfire;

import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.repository.dao.gemfire.GemfireJobInstanceDao;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"batch-gemfire-context.xml","cache-context.xml"})
public class GemfireJobInstanceDaoTest {

	@Resource
	private GemfireJobInstanceDao jobInstanceDao;
	
	@Test
	public void testSave() {
		//set the parameters
		JobParameters parameters = new JobParametersBuilder().addLong("runtime",System.currentTimeMillis()).toJobParameters();
		//try through the mechanism
		JobInstance instance = jobInstanceDao.createJobInstance("test job",parameters);
		//test
		assertNotNull(instance);
		System.out.println(instance.getId());
		//find all
		JobInstance retrieved = jobInstanceDao.getJobInstance("test job",parameters);
		//check
		assertNotNull(retrieved);
	}

}
