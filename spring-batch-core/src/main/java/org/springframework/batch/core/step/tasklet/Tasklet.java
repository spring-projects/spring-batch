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

package org.springframework.batch.core.step.tasklet;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Interface for encapsulating processing logic that is not natural to split
 * into read-(transform)-write phases, such as invoking a system command or a
 * stored procedure.<br/>
 * 
 * Since the batch framework has no visibility inside the {@link #execute()}
 * method, developers should consider implementing {@link StepListener} and
 * check the {@link StepExecution#isTerminateOnly()} value for long lasting
 * processes to enable prompt termination of processing on user request.<br/>
 * 
 * It is expected the read-(transform)-write separation will be appropriate for
 * most cases and developers should implement {@link ItemReader} and
 * {@link ItemWriter} interfaces then (typically extending or composing provided
 * implementations).<br/>
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @author Robert Kasanicky
 * 
 */
public interface Tasklet {

	/**
	 * Encapsulates execution logic of {@link Step}, which is unnatural to
	 * separate into read-(transform)-write phases.
	 * 
	 * @return ExitStatus indicating success or failure
	 * @see org.springframework.batch.repeat.ExitStatus
	 */
	public ExitStatus execute() throws Exception;

}
