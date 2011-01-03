package org.springframework.batch.core.step.splits;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SplitFlowTests {


  @Autowired
  @Qualifier("job")
  private Job job;

  @Autowired
  private JobRepository jobRepository;

  @Test
  public void testSplitJob() throws Exception {
    assertNotNull(job);
    JobExecution jobExecution = jobRepository.createJobExecution(job.getName(), new JobParameters());
    job.execute(jobExecution);
    assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
  /*  assertEquals(4, jobExecution.getStepExecutions().size());
    ArrayList<String> names = new ArrayList<String>(((StepLocator)job).getStepNames());
    Collections.sort(names);
    assertEquals("[s1, s2, s3, s4]", names.toString());*/
  }

 /* public static void main(String[] args) throws Exception {
    ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext("/org/springframework/batch/core/step/splits/SplitFlowTests-context.xml");

    Job job = (Job) classPathXmlApplicationContext.getBean("job");
    JobLauncher jobLauncher = (JobLauncher) classPathXmlApplicationContext.getBean("jobLauncher");

    Map<String, JobParameter> parameterMap = new HashMap<String, JobParameter>();

    *//* parameterMap.put("timestamp", new JobParameter(fileBatchProcessContext.getDate().getTime()));
  parameterMap.put("columnNames", new JobParameter(StringUtils.join(fileBatchProcessContext.getColumnNames(),*//*

    JobParameters jobParameters = new JobParameters(parameterMap);
    jobLauncher.run(job, jobParameters);
  }*/
}
