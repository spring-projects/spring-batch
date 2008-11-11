package org.springframework.batch.sample.domain.football.internal;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.AttributeAccessor;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class ErrorLogTasklet implements Tasklet {

	protected final Log logger = LogFactory.getLog(getClass());
	private SimpleJdbcTemplate simpleJdbcTemplate;

	public RepeatStatus execute(StepContribution contribution, AttributeAccessor attributes) throws Exception {
		this.simpleJdbcTemplate.update("insert into ERROR_LOG values ('Some records were skipped!')");
		return RepeatStatus.FINISHED;
	}

	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
}
