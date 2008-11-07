package org.springframework.batch.sample.domain.nonSequential.internal;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.core.AttributeAccessor;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class ErrorLogWriter implements Tasklet {

	protected final Log logger = LogFactory.getLog(getClass());
	private SimpleJdbcTemplate simpleJdbcTemplate;

	public ExitStatus execute(StepContribution contribution, AttributeAccessor attributes) throws Exception {
		this.simpleJdbcTemplate.update("insert into ERROR_LOG values ('Some records were skipped!')");
		return ExitStatus.FINISHED;
	}

	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
}
