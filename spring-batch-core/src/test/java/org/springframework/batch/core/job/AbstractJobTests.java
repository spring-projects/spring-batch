/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.core.job;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link AbstractJob}.
 */
class AbstractJobTests {

	private final AbstractJob job = new SimpleJob("job");

	@Test
	void testObservationRegistryIsNotSetByDefault() {
		assertNull(this.job.getObservationRegistry());
	}

	@Test
	void testObservationRegistryCanBeSet() {
		this.job.setObservationRegistry(ObservationRegistry.NOOP);

		assertSame(ObservationRegistry.NOOP, this.job.getObservationRegistry());
	}

	@Test
	void testObservationRegistryMustNotBeNull() {
		assertThrows(IllegalArgumentException.class, () -> this.job.setObservationRegistry(null));
	}

}
