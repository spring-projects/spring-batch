/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.batch.core.step.builder;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.xml.DummyItemReader;
import org.springframework.batch.core.configuration.xml.DummyItemWriter;
import org.springframework.batch.core.configuration.xml.DummyJobRepository;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;

import static org.junit.Assert.assertEquals;
public class FaultTolerantStepBuilderTests {

	@Test
	public void faultTolerantReturnsSameInstance() {
		FaultTolerantStepBuilder<Object, Object> builder = new FaultTolerantStepBuilder<>(new StepBuilder("test"));
		assertEquals(builder, builder.faultTolerant());
	}

	@Test
	public void testAnnotationBasedStepExecutionListenerRegistration() {
		// given
		FaultTolerantStepBuilder<Object, Object> faultTolerantStepBuilder = new StepBuilder("myStep")
				.repository(new DummyJobRepository())
				.transactionManager(new ResourcelessTransactionManager())
				.<Object, Object>chunk(5).reader(new DummyItemReader()).writer(new DummyItemWriter()).faultTolerant()
				.listener(new StepBuilderTests.AnnotationBasedStepExecutionListener());

		// when
		Step step = faultTolerantStepBuilder.build();

		// then
		Assert.assertNotNull(step);
	}

}
