package org.springframework.batch.sample;

import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/** 
 * Run the job to read from the "test.inbound" queue, process the messages and write them to the "test.outbound" queue:
 * mvn -q exec:java -Dexec.mainClass="org.springframework.batch.core.launch.support.CommandLineJobRunner" \
 *    -Dexec.arguments="classpath*:/META-INF/spring/jobs/amqp/amqp-example-job.xml,amqp-example-job"
*/

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/amqp-example-job.xml", "/job-runner-context.xml" })
public class AMQPJobFunctionalTests {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;
	private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired
	private JobExplorer jobExplorer;
	
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}


	@Test
	public void testLaunchJob() throws Exception {
		
		jobLauncherTestUtils.launchJob();
		
		int count = jobExplorer.getJobInstances("amqp-example-job", 0, 1).size();

		assertTrue(count > 0);

	}

}
