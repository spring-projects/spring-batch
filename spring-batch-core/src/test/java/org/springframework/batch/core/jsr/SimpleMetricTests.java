package org.springframework.batch.core.jsr;

import static org.junit.Assert.assertEquals;

import javax.batch.runtime.Metric;
import javax.batch.runtime.Metric.MetricType;

import org.junit.Test;

public class SimpleMetricTests {

	@Test(expected=IllegalArgumentException.class)
	public void testNullType() {
		Metric metric = new SimpleMetric(null, 0);
	}

	@Test
	public void test() {
		Metric metric = new SimpleMetric(MetricType.FILTER_COUNT, 3);

		assertEquals(3, metric.getValue());
		assertEquals(MetricType.FILTER_COUNT, metric.getType());
	}
}
