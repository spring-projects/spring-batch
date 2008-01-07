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

package org.springframework.batch.core.domain;


/**
 * Identifier strategy for {@link JobInstance}. Different batch projects can
 * have different approaches and requirements regarding the identity of a job.
 * The minimum requirement is to provide a unique name to identify a job and JobRuntimeParameters.
 * 
 * @author Dave Syer
 * @author Lucas Ward
 * @since 1.0
 */
public interface JobIdentifier {

	/**
	 * A name property for jobs provided by the {@link JobIdentifier} strategy.
	 * 
	 * @return the name of the job
	 */
	public String getName();
	
	/**
	 * A simple getter for the {@link JobRuntimeParameters} that also identify
	 * this job.
	 * 
	 * @return JobRuntimeParameters
	 */
	public JobRuntimeParameters getRuntimeParameters();

}
