package org.springframework.batch.test.sample;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.AttributeAccessor;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class SampleTasklet implements Tasklet {

	@Autowired
	private SimpleJdbcTemplate jdbcTemplate;

	private int id = 0;

	public SampleTasklet(int id) {
		this.id = id;
	}

	public RepeatStatus execute(StepContribution contribution, AttributeAccessor attributes) throws Exception {
		System.err.println("SampleTasklet1.execute()");
		this.jdbcTemplate.update("insert into TESTS(ID, NAME) values (?, 'SampleTasklet" + id + "')", id);
		return RepeatStatus.FINISHED;
	}
}
