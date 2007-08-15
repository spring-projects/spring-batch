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

package org.springframework.batch.core.tasklet;

import org.springframework.batch.core.configuration.StepConfiguration;

/**
 * The primary interface describing the touch-point between the batch developer
 * and a spring-batch execution. The execute method will be called to indicate
 * to the developer that it is time to execute business logic. The value
 * returned from this method will indicate whether or not processing should
 * continue. It is important to note that in the vast majority of cases this
 * class should not be directly implemented by batch developers for processing.
 * Most batch processing is significantly more complex than simple execute and
 * should logically be broken into a minimum of two processes (read and write).
 * However, many architecture teams may find creating their own implementations
 * of this interface useful for differentiating different batch job types, or
 * for creating more flexibility within their batch jobs.
 * 
 * @see StepConfiguration
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public interface Tasklet {

	/**
	 * Primary batch processing driver. All processing of batch business data
	 * should be handled within this method. Any processing which intends to
	 * control the flow of the batch lifecycle by throwing exceptions (such as
	 * BatchCriticalExeception) should throw them within this method. Doing so
	 * outside of this method will prevent the architecture from gracefully
	 * shutting down and providing such features as transaction rollback.
	 * 
	 * @return boolean indicating whether the processing should continue (i.e.
	 * false when data are exhausted).
	 */
	public boolean execute() throws Exception;

}
