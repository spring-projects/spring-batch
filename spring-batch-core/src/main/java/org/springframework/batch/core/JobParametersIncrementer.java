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
package org.springframework.batch.core;

/**
 * Interface for obtaining the next {@link JobParameters} in a sequence.
 * 
 * @author Dave Syer
 * @author Lucas Ward
 * @since 2.0
 */
public interface JobParametersIncrementer {

	/**
	 * Increment the provided parameters. If the input is empty, then this
	 * should return a bootstrap or initial value to be used on the first
	 * instance of a job.
	 * 
	 * @param parameters the last value used
	 * @return the next value to use
	 */
	JobParameters getNext(JobParameters parameters);

}
