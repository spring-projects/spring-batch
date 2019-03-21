/*
 * Copyright 2008-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

	@Override
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

	@Override
	public String getName() {
		return "foo";
	}

	@Override
	public int getStartLimit() {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isAllowStartIfComplete() {
		return false;
	}

}
