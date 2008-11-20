package org.springframework.batch.core.scope;

import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;

public class TestStep implements Step {

	private static StepContext context;

	private Collaborator collaborator;

	public void setCollaborator(Collaborator collaborator) {
		this.collaborator = collaborator;
	}

	public static StepContext getContext() {
		return context;
	}

	public static void reset() {
		context = null;
	}

	public void execute(StepExecution stepExecution) throws JobInterruptedException {
		context = StepSynchronizationManager.getContext();
		setContextFromCollaborator();
		stepExecution.getExecutionContext().put("foo", "changed but it shouldn't affect the collaborator");
		setContextFromCollaborator();
	}

	private void setContextFromCollaborator() {
		if (context != null) {
			context.setAttribute("collaborator", collaborator.getName());
		}
	}

	public String getName() {
		return "foo";
	}

	public int getStartLimit() {
		return Integer.MAX_VALUE;
	}

	public boolean isAllowStartIfComplete() {
		return false;
	}

}
