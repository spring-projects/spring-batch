package org.springframework.batch.core.scope;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.scope.context.JobContext;
import org.springframework.batch.core.scope.context.JobSynchronizationManager;

public class TestJob implements Job {

	private static JobContext context;

	private Collaborator collaborator;

	public void setCollaborator(Collaborator collaborator) {
		this.collaborator = collaborator;
	}

	public static JobContext getContext() {
		return context;
	}

	public static void reset() {
		context = null;
	}

	public void execute(JobExecution stepExecution) {
		context = JobSynchronizationManager.getContext();
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

	public boolean isRestartable() {
		return false;
	}

	public JobParametersIncrementer getJobParametersIncrementer() {
		return null;
	}

	public JobParametersValidator getJobParametersValidator() {
		return null;
	}
}
