/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.core.configuration.support;

import com.vmlens.api.AllInterleavings;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.StepRegistry;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.step.tasklet.TaskletStep;

import java.util.List;

/**
 * @author Thomas Krieger
 */
public class MapStepRegistryCT {

	@Test
	public void unregisterAndGet() throws DuplicateJobException, InterruptedException {
		try (AllInterleavings allInterleavings = new AllInterleavings("MapStepRegistryCT.unregisterAndGet")) {
			while (allInterleavings.hasNext()) {
				StepRegistry registry = new MapStepRegistry();
				registry.register("foo", List.of(new TaskletStep("first")));
				Thread first = new Thread() {
					@Override
					public void run() {
						registry.unregisterStepsFromJob("foo");
					}
				};
				first.start();
				try {
					registry.getStep("foo", "first");
				}
				catch (NoSuchJobException e) {

				}
				first.join();
			}
		}
	}

}
