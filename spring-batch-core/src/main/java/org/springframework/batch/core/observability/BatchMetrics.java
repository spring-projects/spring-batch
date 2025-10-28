/*
 * Copyright 2019-2025 the original author or authors.
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
package org.springframework.batch.core.observability;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

/**
 * Central class for batch metrics. It provides some utility methods like calculating
 * durations and formatting them in a human-readable format.
 * <p>
 * <strong>Only intended for internal use</strong>.
 * </p>
 *
 * @author Mahmoud Ben Hassine
 * @author Glenn Renfro
 * @since 4.2
 */
public final class BatchMetrics {

	public static final String METRICS_PREFIX = "spring.batch.";

	public static final String STATUS_SUCCESS = "SUCCESS";

	public static final String STATUS_FAILURE = "FAILURE";

	public static final String STATUS_COMMITTED = "COMMITTED";

	public static final String STATUS_ROLLED_BACK = "ROLLED_BACK";

	private BatchMetrics() {
	}

	/**
	 * Calculate the duration between two dates.
	 * @param startTime the start time
	 * @param endTime the end time
	 * @return the duration between start time and end time
	 */
	@Nullable public static Duration calculateDuration(@Nullable LocalDateTime startTime, @Nullable LocalDateTime endTime) {
		if (startTime == null || endTime == null) {
			return null;
		}
		return Duration.between(startTime, endTime);
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
		long seconds = duration.toSeconds();
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
