/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.integration.launch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.integration.JobSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Gunnar Hillert
 * @since 1.3
 *
 */
class JobLaunchingGatewayTests {

	@Test
	void testExceptionRaised() throws Exception {

		final Message<JobLaunchRequest> message = MessageBuilder
			.withPayload(new JobLaunchRequest(new JobSupport("testJob"), new JobParameters()))
			.build();

		final JobLauncher jobLauncher = mock();
		when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
			.thenThrow(new JobParametersInvalidException("This is a JobExecutionException."));

		JobLaunchingGateway jobLaunchingGateway = new JobLaunchingGateway(jobLauncher);
		Exception exception = assertThrows(MessageHandlingException.class,
				() -> jobLaunchingGateway.handleMessage(message));
		assertEquals("This is a JobExecutionException.", exception.getCause().getMessage());
	}

}
