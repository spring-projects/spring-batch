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
package org.springframework.batch.core.scope.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.context.SynchronizedAttributeAccessor;
import org.springframework.util.Assert;

/**
 * A context object that can be used to interrogate the current
 * {@link StepExecution} and some of its associated properties using expressions
 * based on bean paths. Has public getters for the step execution and
 * convenience methods for accessing commonly used properties like the
 * {@link ExecutionContext} associated with the step or its enclosing job
 * execution.
 * 
 * @author Dave Syer
 * 
 */
public class StepContext extends SynchronizedAttributeAccessor {

	private StepExecution stepExecution;

	private Map<String, Set<Runnable>> callbacks = new HashMap<String, Set<Runnable>>();

	/**
	 * Create a new instance of {@link StepContext} for this
	 * {@link StepExecution}.
	 * 
	 * @param stepExecution a step execution
	 */
	public StepContext(StepExecution stepExecution) {
		super();
		Assert.notNull(stepExecution, "A StepContext must have a non-null StepExecution");
		this.stepExecution = stepExecution;
	}

	/**
	 * Convenient accessor for current step name identifier. Usually this is the
	 * same as the bean name of the step that is executing (but might not be
	 * e.g. in a partition).
	 * 
	 * @return the step name identifier of the current {@link StepExecution}
	 */
	public String getStepName() {
		return stepExecution.getStepName();
	}

	/**
	 * Convenient accessor for current job name identifier.
	 * 
	 * @return the job name identifier of the enclosing {@link JobInstance}
	 * associated with the current {@link StepExecution}
	 */
	public String getJobName() {
		Assert.state(stepExecution.getJobExecution() != null, "StepExecution does not have a JobExecution");
		Assert.state(stepExecution.getJobExecution().getJobInstance() != null,
				"StepExecution does not have a JobInstance");
		return stepExecution.getJobExecution().getJobInstance().getJobName();
	}

	/**
	 * Convenient accessor for System properties to make it easy to access them
	 * from placeholder expressions.
	 * 
	 * @return the current System properties
	 */
	public Properties getSystemProperties() {
		return System.getProperties();
	}

	/**
	 * @return a map containing the items from the step {@link ExecutionContext}
	 */
	public Map<String, Object> getStepExecutionContext() {
		Map<String, Object> result = new HashMap<String, Object>();
		for (Entry<String, Object> entry : stepExecution.getExecutionContext().entrySet()) {
			result.put(entry.getKey(), entry.getValue());
		}
		return Collections.unmodifiableMap(result);
	}

	/**
	 * @return a map containing the items from the job {@link ExecutionContext}
	 */
	public Map<String, Object> getJobExecutionContext() {
		Map<String, Object> result = new HashMap<String, Object>();
		for (Entry<String, Object> entry : stepExecution.getJobExecution().getExecutionContext().entrySet()) {
			result.put(entry.getKey(), entry.getValue());
		}
		return Collections.unmodifiableMap(result);
	}

	/**
	 * @return a map containing the items from the {@link JobParameters}
	 */
	public Map<String, Object> getJobParameters() {
		Map<String, Object> result = new HashMap<String, Object>();
		for (Entry<String, JobParameter> entry : stepExecution.getJobParameters().getParameters().entrySet()) {
			result.put(entry.getKey(), entry.getValue().getValue());
		}
		return Collections.unmodifiableMap(result);
	}

	/**
	 * Allow clients to register callbacks for clean up on close.
	 * 
	 * @param name the callback id (unique attribute key in this context)
	 * @param callback a callback to execute on close
	 */
	public void registerDestructionCallback(String name, Runnable callback) {
		synchronized (callbacks) {
			Set<Runnable> set = callbacks.get(name);
			if (set == null) {
				set = new HashSet<Runnable>();
				callbacks.put(name, set);
			}
			set.add(callback);
		}
	}

	private void unregisterDestructionCallbacks(String name) {
		synchronized (callbacks) {
			callbacks.remove(name);
		}
	}

	/**
	 * Override base class behaviour to ensure destruction callbacks are
	 * unregistered as well as the default behaviour.
	 * 
	 * @see SynchronizedAttributeAccessor#removeAttribute(String)
	 */
	@Override
	public Object removeAttribute(String name) {
		unregisterDestructionCallbacks(name);
		return super.removeAttribute(name);
	}

	/**
	 * Clean up the context at the end of a step execution. Must be called once
	 * at the end of a step execution to honour the destruction callback
	 * contract from the {@link StepScope}.
	 */
	public void close() {

		List<Exception> errors = new ArrayList<Exception>();

		Map<String, Set<Runnable>> copy = Collections.unmodifiableMap(callbacks);

		for (Entry<String, Set<Runnable>> entry : copy.entrySet()) {
			Set<Runnable> set = entry.getValue();
			for (Runnable callback : set) {
				if (callback != null) {
					/*
					 * The documentation of the interface says that these
					 * callbacks must not throw exceptions, but we don't trust
					 * them necessarily...
					 */
					try {
						callback.run();
					}
					catch (RuntimeException t) {
						errors.add(t);
					}
				}
			}
		}

		if (errors.isEmpty()) {
			return;
		}

		Exception error = errors.get(0);
		if (error instanceof RuntimeException) {
			throw (RuntimeException) error;
		}
		else {
			throw new UnexpectedJobExecutionException("Could not close step context, rethrowing first of "
					+ errors.size() + " exceptions.", error);
		}
	}

	/**
	 * The current {@link StepExecution} that is active in this context.
	 * 
	 * @return the current {@link StepExecution}
	 */
	public StepExecution getStepExecution() {
		return stepExecution;
	}

	/**
	 * @return unique identifier for this context based on the step execution
	 */
	public String getId() {
		Assert.state(stepExecution.getId() != null, "StepExecution has no id.  "
				+ "It must be saved before it can be used in step scope.");
		return "execution#" + stepExecution.getId();
	}

	/**
	 * Extend the base class method to include the step execution itself as a
	 * key (i.e. two contexts are only equal if their step executions are the
	 * same).
	 * 
	 * @see SynchronizedAttributeAccessor#equals(Object)
	 */
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof StepContext))
			return false;
		if (other == this)
			return true;
		StepContext context = (StepContext) other;
		if (context.stepExecution == stepExecution) {
			return true;
		}
		return stepExecution.equals(context.stepExecution);
	}

	/**
	 * Overrides the default behaviour to provide a hash code based only on the
	 * step execution.
	 * 
	 * @see SynchronizedAttributeAccessor#hashCode()
	 */
	@Override
	public int hashCode() {
		return stepExecution.hashCode();
	}

	@Override
	public String toString() {
		return super.toString() + ", stepExecutionContext=" + getStepExecutionContext() + ", jobExecutionContext="
				+ getJobExecutionContext() + ", jobParameters=" + getJobParameters();
	}

}
