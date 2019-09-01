/*
 * Copyright 2006-2019 the original author or authors.
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
package org.springframework.batch.core.step.item;

import java.lang.reflect.Constructor;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.lang.Nullable;

/**
 * @author Dan Garrette
 * @since 2.0.2
 */
public class ExceptionThrowingTaskletStub implements Tasklet {

	private int maxTries = 4;

	protected Log logger = LogFactory.getLog(getClass());

	private List<Integer> committed = TransactionAwareProxyFactory.createTransactionalList();

	private Constructor<? extends Exception> exception;

	public ExceptionThrowingTaskletStub() throws Exception {
		exception = SkippableRuntimeException.class.getConstructor(String.class);
	}

	public void setExceptionType(Class<? extends Exception> exceptionType) throws Exception {
		exception = exceptionType.getConstructor(String.class);
	}

	public List<Integer> getCommitted() {
		return committed;
	}

	public void clear() {
		committed.clear();
	}

	@Nullable
	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		committed.add(1);
		if (committed.size()>=maxTries) {
			return RepeatStatus.FINISHED;
		}
		throw exception.newInstance("Expected exception");
	}
}
