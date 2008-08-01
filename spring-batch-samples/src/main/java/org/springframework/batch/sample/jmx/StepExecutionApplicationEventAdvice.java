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

package org.springframework.batch.sample.jmx;

import org.aspectj.lang.JoinPoint;
import org.springframework.batch.core.StepExecution;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

/**
 * Wraps calls for methods taking {@link StepExecution} as an argument and
 * publishes notifications in the form of {@link ApplicationEvent}.
 * 
 * @author Dave Syer
 */
public class StepExecutionApplicationEventAdvice implements ApplicationEventPublisherAware {

	private ApplicationEventPublisher applicationEventPublisher;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher(org.springframework.context.ApplicationEventPublisher)
	 */
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public void before(JoinPoint jp, StepExecution stepExecution) {
		String msg = "Before: " + jp.toShortString() + " with: " + stepExecution;
		publish(jp.getTarget(), msg);
	}

	public void after(JoinPoint jp, StepExecution stepExecution) {
		String msg = "After: " + jp.toShortString() + " with: " + stepExecution;
		publish(jp.getTarget(), msg);
	}

	public void onError(JoinPoint jp, StepExecution stepExecution, Throwable t) {
		String msg = "Error in: " + jp.toShortString() + " with: " + stepExecution + " (" + t.getClass() + ":" + t.getMessage() + ")";
		publish(jp.getTarget(), msg);
	}

	/*
	 * Publish a {@link SimpleMessageApplicationEvent} with the given
	 * parameters.
	 */
	private void publish(Object source, String message) {
		applicationEventPublisher.publishEvent(new SimpleMessageApplicationEvent(source, message));
	}

}
