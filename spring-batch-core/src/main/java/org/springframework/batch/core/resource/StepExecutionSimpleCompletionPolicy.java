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

package org.springframework.batch.core.resource;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.util.Assert;

/**
 * <p>
 * A {@link CompletionPolicy} that picks up a commit interval from
 * {@link JobParameters} by listening to the start of a step. Use anywhere that
 * a {@link CompletionPolicy} can be used (usually at the chunk level in a
 * step), and inject as a {@link StepExecutionListener} into the surrounding
 * step. N.B. only after the step has started will the completion policy be
 * usable.
 * </p>
 * 
 * <p>
 * It is easier and probably preferable to simply declare the chunk with a
 * commit-interval that is a late-binding expression (e.g.
 * <code>#{jobParameters['commit.interval']}</code>). That feature is available
 * from of Spring Batch 2.1.7.
 * </p>
 * 
 * @author Dave Syer
 * 
 * @see CompletionPolicy
 */
public class StepExecutionSimpleCompletionPolicy extends StepExecutionListenerSupport implements CompletionPolicy {

	private CompletionPolicy delegate;

	private String keyName = "commit.interval";

	/**
	 * Public setter for the key name of a Long value in the
	 * {@link JobParameters} that will contain a commit interval. Defaults to
	 * "commit.interval".
	 * @param keyName the keyName to set
	 */
	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

	/**
	 * Set up a {@link SimpleCompletionPolicy} with a commit interval taken from
	 * the {@link JobParameters}. If there is a Long parameter with the given
	 * key name, the intValue of this parameter is used. If not an exception
	 * will be thrown.
	 * 
	 * @see org.springframework.batch.core.listener.StepExecutionListenerSupport#beforeStep(org.springframework.batch.core.StepExecution)
	 */
	public void beforeStep(StepExecution stepExecution) {
		JobParameters jobParameters = stepExecution.getJobParameters();
		Assert.state(jobParameters.getParameters().containsKey(keyName),
				"JobParameters do not contain Long parameter with key=[" + keyName + "]");
		delegate = new SimpleCompletionPolicy((int) jobParameters.getLong(keyName));
	}

	/**
	 * @param context
	 * @param result
	 * @return true if the commit interval has been reached or the result
	 * indicates completion
	 * @see CompletionPolicy#isComplete(RepeatContext, RepeatStatus)
	 */
	public boolean isComplete(RepeatContext context, RepeatStatus result) {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.isComplete(context, result);
	}

	/**
	 * @param context
	 * @return if the commit interval has been reached
	 * @see org.springframework.batch.repeat.CompletionPolicy#isComplete(org.springframework.batch.repeat.RepeatContext)
	 */
	public boolean isComplete(RepeatContext context) {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.isComplete(context);
	}

	/**
	 * @param parent
	 * @return a new {@link RepeatContext}
	 * @see org.springframework.batch.repeat.CompletionPolicy#start(org.springframework.batch.repeat.RepeatContext)
	 */
	public RepeatContext start(RepeatContext parent) {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.start(parent);
	}

	/**
	 * @param context
	 * @see org.springframework.batch.repeat.CompletionPolicy#update(org.springframework.batch.repeat.RepeatContext)
	 */
	public void update(RepeatContext context) {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		delegate.update(context);
	}

	/**
	 * Delegates to the wrapped {@link CompletionPolicy} if set, otherwise
	 * returns the value of {@link #setKeyName(String)}.
	 */
	public String toString() {
		return (delegate == null) ? keyName : delegate.toString();
	}

}
