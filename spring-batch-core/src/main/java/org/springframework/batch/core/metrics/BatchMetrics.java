/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.batch.core.metrics;

import java.util.Arrays;
import java.util.function.Supplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

/**
 * Main entry point to interact with Micrometer's {@link Metrics#globalRegistry}.
 * Provides common metrics such as {@link Timer}, {@link Counter} and {@link Gauge}.
 *
 * @author Mahmoud Ben Hassine
 */
public class BatchMetrics {

	public static final String METRICS_PREFIX = "spring.batch.";

	public static final String STATUS_SUCCESS = "SUCCESS";

	public static final String STATUS_FAILURE = "FAILURE";

	/**
	 * Create a {@link Timer}.
	 *
	 * @param name        of the timer. Will be prefixed with {@link BatchMetrics#METRICS_PREFIX}.
	 * @param description of the timer
	 * @param tags        of the timer
	 * @return a new timer instance
	 */
	public static Timer createTimer(String name, String description, Tag... tags) {
		return Timer.builder(METRICS_PREFIX + name)
				.description(description)
				.tags(Arrays.asList(tags))
				.register(Metrics.globalRegistry);
	}

	/**
	 * Create a new {@link Timer.Sample}.
	 *
	 * @return a new timer sample instance
	 */
	public static Timer.Sample createTimerSample() {
		return Timer.start(Metrics.globalRegistry);
	}

	/**
	 * Create a new {@link LongTaskTimer}.
	 *
	 * @param name        of the long task timer. Will be prefixed with {@link BatchMetrics#METRICS_PREFIX}.
	 * @param description of the long task timer.
	 * @param tags        of the timer
	 * @return a new long task timer instance
	 */
	public static LongTaskTimer createLongTaskTimer(String name, String description, Tag... tags) {
		return LongTaskTimer.builder(METRICS_PREFIX + name)
				.description(description)
				.tags(Arrays.asList(tags))
				.register(Metrics.globalRegistry);
	}

	/**
	 * Create a new {@link Counter}.
	 *
	 * @param name        of the counter. Will be prefixed with {@link BatchMetrics#METRICS_PREFIX}.
	 * @param description of the counter
	 * @param tags        of the counter
	 * @return a new counter instance
	 */
	public static Counter createCounter(String name, String description, Tag... tags) {
		return Counter.builder(METRICS_PREFIX + name)
				.description(description)
				.tags(Arrays.asList(tags))
				.register(Metrics.globalRegistry);
	}

	/**
	 * Create a new {@link Gauge}.
	 *
	 * @param name        of the gauge. Will be prefixed with {@link BatchMetrics#METRICS_PREFIX}.
	 * @param description of the gauge
	 * @param supplier    A supplier that yields a value for the gauge.
	 * @param tags        of the gauge
	 * @return a new gauge instance
	 */
	public static Gauge createGauge(String name, String description, Supplier<Number> supplier, Tag... tags) {
		return Gauge.builder(METRICS_PREFIX + name, supplier)
				.description(description)
				.tags(Arrays.asList(tags))
				.register(Metrics.globalRegistry);
	}

}
