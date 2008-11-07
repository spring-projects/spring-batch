package org.springframework.batch.core.annotation;


import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"component-scan-context.xml"})
public class StepComponentBeanPostProcessorIntegrationTests {

	@Autowired
	private SimpleJob job;
	
	@Autowired
	private JobLauncher jobLauncher;
	
	@Test
	public void testListener() throws Exception{
		
		jobLauncher.run(job, new JobParametersBuilder().addDate("run.date", new Date()).toJobParameters());
		
	//	assertTrue(TestComponent.isAfterStepCalled());
	//	assertTrue(TestComponent.isBeforeStepCalled());
	}
}
