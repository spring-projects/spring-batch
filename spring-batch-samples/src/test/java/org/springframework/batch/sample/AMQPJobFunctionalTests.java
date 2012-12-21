package org.springframework.batch.sample;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * <p>Ensure a RabbitMQ instance is running, modifying default.amqp.properties if needed. Execute the
 * {@link org.springframework.batch.sample.rabbitmq.amqp.AmqpMessageProducer#main(String[])} method
 * in order for messages will be written to the "test.inbound" queue.</p>
 *
 * <p>Run this test and the job will read those messages, process them and write them to the "test.outbound"
 * queue for inspection.</p>
*/

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/amqp-example-job.xml", "/job-runner-context.xml" })
public class AMQPJobFunctionalTests {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;
	@Autowired
	private JobExplorer jobExplorer;

	@Test
	public void testLaunchJob() throws Exception {

		jobLauncherTestUtils.launchJob();

		int count = jobExplorer.getJobInstances("amqp-example-job", 0, 1).size();

		assertTrue(count > 0);

	}

}
