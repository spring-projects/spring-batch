/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.execution.step.simple;

import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.core.tasklet.Recoverable;
import org.springframework.batch.io.Skippable;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Adds some recovery behaviour to {@link SimpleStepExecutor}.
 * 
 * @author Dave Syer
 * 
 */
public class DefaultStepExecutor extends SimpleStepExecutor {

	/**
	 * Extends {@link SimpleStepExecutor#doTaskletProcessing(Tasklet, StepInstance)} to
	 * add some basic recovery behaviour. If the {@link Tasklet} implements
	 * {@link Recoverable} <em>and</em> {@link Skippable} then the recovery
	 * and skip methods are called. The recovery is done in a new transaction,
	 * started with propagation
	 * {@link TransactionDefinition#PROPAGATION_REQUIRES_NEW} so that the
	 * inevitable rollback on the main processing loop does not cause the
	 * recovery to roll back as well.
	 * 
	 * @throws Exception whenever {@link SimpleStepExecutor} would, but takes
	 * the recovery path first.
	 * 
	 * @see org.springframework.batch.execution.step.simple.SimpleStepExecutor#doTaskletProcessing(org.springframework.batch.core.tasklet.Tasklet,
	 * org.springframework.batch.core.domain.StepInstance)
	 */
	protected ExitStatus doTaskletProcessing(Tasklet module, final StepExecution step) throws Exception {

		ExitStatus exitStatus = ExitStatus.CONTINUABLE;

		try {

			exitStatus = super.doTaskletProcessing(module, step);

		}
		catch (final Exception e) {

			if (module instanceof Recoverable && module instanceof Skippable) {
				final Recoverable recoverable = (Recoverable) module;
				new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(
						TransactionDefinition.PROPAGATION_REQUIRES_NEW)).execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						recoverable.recover(e);
						return null;
					}
				});
			}
			if (module instanceof Skippable) {
				((Skippable) module).skip();
			}

			// Rethrow so that outer transaction is rolled back properly
			throw e;

		}

		return exitStatus;
	}

}
