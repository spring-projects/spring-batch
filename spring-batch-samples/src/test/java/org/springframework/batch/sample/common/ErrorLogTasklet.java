package org.springframework.batch.sample.common;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class ErrorLogTasklet implements Tasklet, StepExecutionListener {

	protected final Log logger = LogFactory.getLog(getClass());
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	private String jobName;
	private String stepName;

	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		this.simpleJdbcTemplate.update("insert into ERROR_LOG values ('"+jobName+"', '"+stepName+"', 'Some records were skipped!')");
		return RepeatStatus.FINISHED;
	}

	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	public ExitStatus afterStep(StepExecution stepExecution) {
		// TODO Auto-generated method stub
		return null;
	}

	public void beforeStep(StepExecution stepExecution) {
		this.jobName = stepExecution.getJobExecution().getJobInstance().getJobName().trim();
		this.stepName = (String)stepExecution.getJobExecution().getExecutionContext().get("stepName");
		
	}
}
