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
package org.springframework.batch.repeat.interceptor;

import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatInterceptor;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

/**
 * @author Dave Syer
 * 
 */
public class ApplicationEventPublisherRepeatInterceptor implements ApplicationEventPublisherAware, RepeatInterceptor {

	private ApplicationEventPublisher applicationEventPublisher;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher(org.springframework.context.ApplicationEventPublisher)
	 */
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.repeat.RepeatInterceptor#after(org.springframework.batch.repeat.RepeatContext,
	 * java.lang.Object)
	 */
	public void after(RepeatContext context, Object result) {
		publish(context, "After repeat callback with result=[" + result + "]", RepeatOperationsApplicationEvent.AFTER);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.repeat.RepeatInterceptor#before(org.springframework.batch.repeat.RepeatContext)
	 */
	public void before(RepeatContext context) {
		publish(context, "Before repeat callback", RepeatOperationsApplicationEvent.BEFORE);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.repeat.RepeatInterceptor#close(org.springframework.batch.repeat.RepeatContext)
	 */
	public ExitStatus close(RepeatContext context) {
		publish(context, "Closed repeat context with batch complete", RepeatOperationsApplicationEvent.CLOSE);
		return ExitStatus.CONTINUABLE;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.repeat.RepeatInterceptor#onError(org.springframework.batch.repeat.RepeatContext,
	 * java.lang.Throwable)
	 */
	public void onError(RepeatContext context, Throwable e) {
		publish(context, "Error in repeat operations with Throwable type=["+e.getClass()+"], message=["+e.getMessage()+"]", RepeatOperationsApplicationEvent.ERROR);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.repeat.RepeatInterceptor#open(org.springframework.batch.repeat.RepeatContext)
	 */
	public void open(RepeatContext context) {
		publish(context, "Repeat operations opened", RepeatOperationsApplicationEvent.OPEN);
	}

	/**
	 * Publish a {@link RepeatOperationsApplicationEvent} with the given
	 * parameters.
	 * 
	 * @param context the current batch context
	 * @param message the message to publish
	 * @param type the type of event to publish
	 */
	private void publish(RepeatContext context, String message, int type) {
		applicationEventPublisher.publishEvent(new RepeatOperationsApplicationEvent(context, message, type));
	}

}
