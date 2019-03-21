/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.core.jsr;

import static org.junit.Assert.assertEquals;

import javax.batch.runtime.Metric;
import javax.batch.runtime.Metric.MetricType;

import org.junit.Test;

public class SimpleMetricTests {

	@Test(expected=IllegalArgumentException.class)
	public void testNullType() {
		new SimpleMetric(null, 0);
	}

	@Test
	public void test() {
		Metric metric = new SimpleMetric(MetricType.FILTER_COUNT, 3);

		assertEquals(3, metric.getValue());
		assertEquals(MetricType.FILTER_COUNT, metric.getType());
	}
}
