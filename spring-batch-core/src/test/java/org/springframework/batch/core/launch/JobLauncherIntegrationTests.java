package org.springframework.batch.core.launch;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JobLauncherIntegrationTests {

	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private Job job;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	public void testLaunchAndRelaunch() throws Exception {

		int before = jdbcTemplate.queryForInt("select count(*) from BATCH_JOB_INSTANCE");

		JobExecution jobExecution = launch(true,0);
		launch(false, jobExecution.getId());
		launch(false, jobExecution.getId());

		int after = jdbcTemplate.queryForInt("select count(*) from BATCH_JOB_INSTANCE");
		assertEquals(before+1, after);

	}

	private JobExecution launch(boolean start, long jobExecutionId) throws Exception {

		if (start) {

			Calendar c = Calendar.getInstance();
			JobParametersBuilder builder = new JobParametersBuilder();
			builder.addDate("TIMESTAMP", c.getTime());
			JobParameters jobParameters = builder.toJobParameters();

			return jobLauncher.run(job, jobParameters);

		} else {

			JdbcJobExecutionDao dao = new JdbcJobExecutionDao();
			dao.setJdbcTemplate(jdbcTemplate);
			JobExecution execution = dao.getJobExecution(jobExecutionId);

			if (execution != null) {
				return jobLauncher.run(job, execution.getJobParameters());
			}

			return null;

		}

	}

}
