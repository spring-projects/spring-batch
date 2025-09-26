/*
 * Copyright 2019-2025 the original author or authors.
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
package org.springframework.batch.core.observability;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mahmoud Ben Hassine
 */
class BatchMetricsTests {

	@Test
	void testCalculateDuration() {
		LocalDateTime startTime = LocalDateTime.now();
		LocalDateTime endTime = startTime.plus(2, ChronoUnit.HOURS)
			.plus(31, ChronoUnit.MINUTES)
			.plus(12, ChronoUnit.SECONDS)
			.plus(42, ChronoUnit.MILLIS);

		Duration duration = BatchMetrics.calculateDuration(startTime, endTime);
		Duration expectedDuration = Duration.ofMillis(42).plusSeconds(12).plusMinutes(31).plusHours(2);
		assertEquals(expectedDuration, duration);
	}

	@Test
	void testCalculateDurationWhenNoStartTime() {
		Duration duration = BatchMetrics.calculateDuration(null, LocalDateTime.now());
		assertNull(duration);
	}

	@Test
	void testCalculateDurationWhenNoEndTime() {
		Duration duration = BatchMetrics.calculateDuration(LocalDateTime.now(), null);
		assertNull(duration);
	}

	@Test
	void testFormatValidDuration() {
		Duration duration = Duration.ofMillis(42).plusSeconds(12).plusMinutes(31).plusHours(2);
		String formattedDuration = BatchMetrics.formatDuration(duration);
		assertEquals("2h31m12s42ms", formattedDuration);
	}

	@Test
	void testFormatValidDurationWithoutHours() {
		Duration duration = Duration.ofMillis(42).plusSeconds(12).plusMinutes(31);
		String formattedDuration = BatchMetrics.formatDuration(duration);
		assertEquals("31m12s42ms", formattedDuration);
	}

	@Test
	void testFormatValidDurationWithoutMinutes() {
		Duration duration = Duration.ofMillis(42).plusSeconds(12);
		String formattedDuration = BatchMetrics.formatDuration(duration);
		assertEquals("12s42ms", formattedDuration);
	}

	@Test
	void testFormatValidDurationWithoutSeconds() {
		Duration duration = Duration.ofMillis(42);
		String formattedDuration = BatchMetrics.formatDuration(duration);
		assertEquals("42ms", formattedDuration);
	}

	@Test
	void testFormatNegativeDuration() {
		Duration duration = Duration.ofMillis(-1);
		String formattedDuration = BatchMetrics.formatDuration(duration);
		assertTrue(formattedDuration.isEmpty());
	}

	@Test
	void testFormatZeroDuration() {
		String formattedDuration = BatchMetrics.formatDuration(Duration.ZERO);
		assertTrue(formattedDuration.isEmpty());
	}

	@Test
	void testFormatNullDuration() {
		String formattedDuration = BatchMetrics.formatDuration(null);
		assertTrue(formattedDuration.isEmpty());
	}

}
