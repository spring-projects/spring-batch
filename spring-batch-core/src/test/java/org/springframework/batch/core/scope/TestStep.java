package org.springframework.batch.core.scope;

import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;

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
			context.setAttribute("collaborator.class", collaborator.getClass().toString());
			if (collaborator.getParent()!=null) {
				context.setAttribute("parent", collaborator.getParent().getName());
				context.setAttribute("parent.class", collaborator.getParent().getClass().toString());
			}
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
