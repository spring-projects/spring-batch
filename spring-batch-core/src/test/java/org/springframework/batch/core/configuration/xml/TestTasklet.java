package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.AttributeAccessor;

public class TestTasklet extends AbstractTestComponent implements Tasklet {

	public RepeatStatus execute(StepContribution contribution,
			AttributeAccessor attributes) throws Exception {
		executed = true;
		contribution.setExitStatus(ExitStatus.FINISHED);
		return RepeatStatus.FINISHED;  
	}  

}
