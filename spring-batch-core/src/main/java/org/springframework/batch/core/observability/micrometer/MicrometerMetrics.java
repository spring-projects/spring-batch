/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.core.observability.micrometer;

import java.util.Arrays;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.batch.core.observability.BatchMetrics;

/**
 * Central class for Micrometer metrics. <strong>Only intended for internal use</strong>.
 *
 * @author Mahmoud Ben Hassine
 * @since 6.0
 */
public final class MicrometerMetrics {

	private MicrometerMetrics() {
	}

	/**
	 * Create a new {@link Observation}. It's not started, you must explicitly call
	 * {@link Observation#start()} to start it.
	 * @param name of the observation
	 * @param observationRegistry the observation registry to use
	 * @return a new observation instance
	 * @since 6.0
	 */
	public static Observation createObservation(String name, ObservationRegistry observationRegistry) {
		return Observation.createNotStarted(name, observationRegistry);
	}

	/**
	 * Create a {@link Timer}.
	 * @param meterRegistry the meter registry to use
	 * @param name of the timer. Will be prefixed with
	 * {@link BatchMetrics#METRICS_PREFIX}.
	 * @param description of the timer
	 * @param tags of the timer
	 * @return a new timer instance
	 */
	public static Timer createTimer(MeterRegistry meterRegistry, String name, String description, Tag... tags) {
		return Timer.builder(BatchMetrics.METRICS_PREFIX + name)
			.description(description)
			.tags(Arrays.asList(tags))
			.register(meterRegistry);
	}

	/**
	 * Create a {@link Counter}.
	 * @param meterRegistry the meter registry to use
	 * @param name of the counter. Will be prefixed with
	 * {@link BatchMetrics#METRICS_PREFIX}.
	 * @param description of the counter
	 * @param tags of the counter
	 * @return a new timer instance
	 */
	public static Counter createCounter(MeterRegistry meterRegistry, String name, String description, Tag... tags) {
		return Counter.builder(BatchMetrics.METRICS_PREFIX + name)
			.description(description)
			.tags(Arrays.asList(tags))
			.register(meterRegistry);
	}

	/**
	 * Create a new {@link Timer.Sample}.
	 * @param meterRegistry the meter registry to use
	 * @return a new timer sample instance
	 */
	public static Timer.Sample createTimerSample(MeterRegistry meterRegistry) {
		return Timer.start(meterRegistry);
	}

	/**
	 * Create a new {@link LongTaskTimer}.
	 * @param meterRegistry the meter registry to use
	 * @param name of the long task timer. Will be prefixed with
	 * {@link BatchMetrics#METRICS_PREFIX}.
	 * @param description of the long task timer.
	 * @param tags of the timer
	 * @return a new long task timer instance
	 */
	public static LongTaskTimer createLongTaskTimer(MeterRegistry meterRegistry, String name, String description,
			Tag... tags) {
		return LongTaskTimer.builder(BatchMetrics.METRICS_PREFIX + name)
			.description(description)
			.tags(Arrays.asList(tags))
			.register(meterRegistry);
	}

}
