package org.springframework.batch.test.sample;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class SampleTasklet implements Tasklet {

	@Autowired
	private SimpleJdbcTemplate jdbcTemplate;

	private JobExecution jobExecution;

	private int id = 0;

	public boolean jobContextEntryFound = false;

	public SampleTasklet(int id) {
		this.id = id;
	}

	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		this.jdbcTemplate.update("insert into TESTS(ID, NAME) values (?, 'SampleTasklet" + id + "')", id);

		if (jobExecution != null) {
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			if (jobContext.containsKey("key1")) {
				jobContextEntryFound = true;
			}
		}

		return RepeatStatus.FINISHED;
	}

	@BeforeStep
	public void storeJobExecution(StepExecution stepExecution) {
		this.jobExecution = stepExecution.getJobExecution();
	}
}
