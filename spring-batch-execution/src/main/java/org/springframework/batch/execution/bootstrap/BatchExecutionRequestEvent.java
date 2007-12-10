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
package org.springframework.batch.execution.bootstrap;

import org.springframework.batch.repeat.interceptor.RepeatOperationsApplicationEvent;
import org.springframework.context.ApplicationEvent;

/**
 * {@link ApplicationEvent} that encodes a request from the execution layer to a
 * running job.
 * 
 * @author Dave Syer
 * 
 */
public class BatchExecutionRequestEvent extends ApplicationEvent {

	/**
	 * Constructor for {@link BatchExecutionRequestEvent}. The source is the
	 * execution layer service implementation that is sending the signal.<br/>
	 * 
	 * TODO: the source should be Serializable so really it should be just a
	 * message about the request?
	 * 
	 * Currently encodes a request to publish back a
	 * {@link RepeatOperationsApplicationEvent}. Could be extended in the
	 * future to narrow the request to ask for specific information to be
	 * published back.
	 */
	public BatchExecutionRequestEvent(Object source) {
		super(source);
	}
}
