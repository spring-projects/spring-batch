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

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

import org.springframework.lang.Nullable;

/**
 * Central class for batch metrics. It provides:
 *
 * <ul>
 *     <li>the main entry point to interact with Micrometer's {@link Metrics#globalRegistry}
 *     with common metrics such as {@link Timer} and {@link LongTaskTimer}.</li>
 *     <li>Some utility methods like calculating durations and formatting them in
 *     a human readable format.</li>
 * </ul>
 *
 * Only intended for internal use.
 *
 * @author Mahmoud Ben Hassine
 * @since 4.2
 */
public final class BatchMetrics {

	private static final String METRICS_PREFIX = "spring.batch.";

	public static final String STATUS_SUCCESS = "SUCCESS";

	public static final String STATUS_FAILURE = "FAILURE";

	private BatchMetrics() {}

	/**
	 * Create a {@link Timer}.
	 * @param name of the timer. Will be prefixed with {@link BatchMetrics#METRICS_PREFIX}.
	 * @param description of the timer
	 * @param tags of the timer
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
	 * @return a new timer sample instance
	 */
	public static Timer.Sample createTimerSample() {
		return Timer.start(Metrics.globalRegistry);
	}

	/**
	 * Create a new {@link LongTaskTimer}.
	 * @param name of the long task timer. Will be prefixed with {@link BatchMetrics#METRICS_PREFIX}.
	 * @param description of the long task timer.
	 * @param tags of the timer
	 * @return a new long task timer instance
	 */
	public static LongTaskTimer createLongTaskTimer(String name, String description, Tag... tags) {
		return LongTaskTimer.builder(METRICS_PREFIX + name)
				.description(description)
				.tags(Arrays.asList(tags))
				.register(Metrics.globalRegistry);
	}

	/**
	 * Calculate the duration between two dates.
	 * @param startTime the start time
	 * @param endTime the end time
	 * @return the duration between start time and end time
	 */
	@Nullable
	public static Duration calculateDuration(@Nullable Date startTime, @Nullable Date endTime) {
		if (startTime == null || endTime == null) {
			return null;
		}
		return Duration.between(startTime.toInstant(), endTime.toInstant());
	}

	/**
	 * Format a duration in a human readable format like: 2h32m15s10ms.
	 * @param duration to format
	 * @return A human readable duration
	 */
	public static String formatDuration(@Nullable Duration duration) {
		if (duration == null || duration.isZero() || duration.isNegative()) {
			return "";
		}
		StringBuilder formattedDuration = new StringBuilder();
		long hours = duration.toHours();
		long minutes = duration.toMinutes();
		long seconds = duration.getSeconds();
		long millis = duration.toMillis();
		if (hours != 0) {
			formattedDuration.append(hours).append("h");
		}
		if (minutes != 0) {
			formattedDuration.append(minutes - TimeUnit.HOURS.toMinutes(hours)).append("m");
		}
		if (seconds != 0) {
			formattedDuration.append(seconds - TimeUnit.MINUTES.toSeconds(minutes)).append("s");
		}
		if (millis != 0) {
			formattedDuration.append(millis - TimeUnit.SECONDS.toMillis(seconds)).append("ms");
		}
		return formattedDuration.toString();
	}

}
