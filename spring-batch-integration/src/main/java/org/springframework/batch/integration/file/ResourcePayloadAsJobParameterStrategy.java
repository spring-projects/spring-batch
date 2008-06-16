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
package org.springframework.batch.integration.file;

import java.io.IOException;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.core.io.Resource;
import org.springframework.integration.message.Message;

/**
 * @author Dave Syer
 * 
 */
public class ResourcePayloadAsJobParameterStrategy implements MessageToJobParametersStrategy {

	/**
	 * The key name for the job parameter that will be a URL for the input file
	 */
	public static final String FILE_INPUT_PATH = "input.file.path";

	/**
	 * Convert a message payload which is a {@link Resource} to its URL
	 * representation and load that into a job parameter.
	 * 
	 * @see org.springframework.batch.integration.file.MessageToJobParametersStrategy#getJobParameters(org.springframework.integration.message.Message)
	 */
	public JobParameters getJobParameters(Message<?> message) {
		JobParametersBuilder builder = new JobParametersBuilder();
		Resource resource = (Resource) message.getPayload();
		try {
			builder.addString(FILE_INPUT_PATH, resource.getURL().toExternalForm());
		}
		catch (IOException e) {
			throw new ItemStreamException("Could not create URL for resource: [" + resource + "]", e);
		}
		return builder.toJobParameters();
	}

}
