package org.springframework.batch.sandbox;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.UUID;

/**
 * @author Josh Long
 * @see {@link org.springframework.batch.core.partition.support.PartitionStep}
 */
public class PartionedStepClient {
	public static void main (String [] args ) throws Throwable {


		ClassPathXmlApplicationContext cax = new ClassPathXmlApplicationContext ("org/springframework/batch/sandbox/ps1.xml");

		JobLauncher jobLauncher = (JobLauncher) cax.getBean("jobLauncher");
		Job job = (Job) cax.getBean("partitionedJob") ;


		JobParameters parms = new JobParametersBuilder().addString("uid", UUID.randomUUID().toString()).toJobParameters();
		JobExecution execution = jobLauncher.run( job, parms);

	}
}
