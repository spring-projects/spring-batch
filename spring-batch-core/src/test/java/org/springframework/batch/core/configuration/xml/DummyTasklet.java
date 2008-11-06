package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.core.AttributeAccessor;

public class DummyTasklet implements Tasklet {

	public ExitStatus execute(StepContribution contribution,
			AttributeAccessor attributes) throws Exception {
		return ExitStatus.FINISHED;  
	}  

}
