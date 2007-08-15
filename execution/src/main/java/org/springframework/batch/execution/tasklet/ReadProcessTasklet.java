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

package org.springframework.batch.execution.tasklet;

import org.springframework.batch.core.tasklet.Tasklet;

/**
 * Provides the basic batch module for reading and processing data.
 * Implementations of this class will be handling both the input and output of
 * data within one class. Developers should ensure that all reading is done
 * before returning from the read() method. This is to ensure all data has been
 * read first, before beginning to process. It is possibly detrimental to
 * performance if processing begins when records still need to be read, because
 * any writing of output will put the transaction in a volatile state, since
 * errors with any additional input will need to cause a rollback, rather than
 * simply skipping that record.
 * 
 * @author Lucas Ward
 * 
 */
public abstract class ReadProcessTasklet implements Tasklet {

	/**
	 * Required for implementation of the {@link Tasklet} interface. The boolean returned
	 * from the abstract read method will be returned to the {@link Tasklet}, to indicate
	 * whether or not processing should continue.
	 */
	public final boolean execute() throws Exception {
		if (!read()) {
			return false;
		}
		process();
		return true;
	}

	/**
	 * Abstract read method to be implemented by batch developers. All data
	 * should be read from within this method and a boolean indicated whether or
	 * not processing should continue should be returned.
	 * 
	 * @return boolean indicating whether or not processing should continue.
	 */
	public abstract boolean read() throws Exception;

	/**
	 * Abstract process method to be implemented by batch developers. All
	 * processing and writing out of data should be done within this method.
	 */
	public abstract void process() throws Exception;
}
