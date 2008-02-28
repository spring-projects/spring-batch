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
package org.springframework.batch.execution.scope;

import org.springframework.batch.core.domain.JobParameters;

/**
 * Marker interface for callback injecting {@link JobParameters}. A Spring bean
 * which is step scoped will be injected with the {@link JobParameters} when it
 * is instantiated. In most cases this will require the use of
 * &lt;aop:scoped-proxy&gt; when the bean is used as a dependency in a
 * singleton.
 * 
 * @author Dave Syer
 * 
 */
public interface JobParametersAware {

	/**
	 * Callback method for injection of {@link JobParameters}.
	 * 
	 * @param jobParameters the {@link JobParameters} to set.
	 */
	void setJobParameters(JobParameters jobParameters);

}
