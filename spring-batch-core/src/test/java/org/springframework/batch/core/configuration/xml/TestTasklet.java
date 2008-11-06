package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.core.AttributeAccessor;

public class TestTasklet extends AbstractTestComponent implements Tasklet {

	public ExitStatus execute(StepContribution contribution,
			AttributeAccessor attributes) throws Exception {
		executed = true;
		return ExitStatus.FINISHED;  
	}  

}
