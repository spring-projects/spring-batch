/*
 * Copyright 2021-2025 the original author or authors.
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
package org.springframework.batch.samples.metrics;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrometheusConfiguration {

	@Value("${prometheus.job.name}")
	private String prometheusJobName;

	@Value("${prometheus.grouping.key}")
	private String prometheusGroupingKey;

	@Value("${prometheus.pushgateway.url}")
	private String prometheusPushGatewayUrl;

	@Bean
	public PrometheusRegistry prometheusRegistry() {
		return new PrometheusRegistry();
	}

	@Bean
	public PrometheusMeterRegistry meterRegistry(PrometheusRegistry prometheusRegistry) {
		return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, prometheusRegistry, Clock.SYSTEM);
	}

	@Bean
	public PushGateway pushGateway(PrometheusRegistry prometheusRegistry) {
		return PushGateway.builder()
			.address(prometheusPushGatewayUrl)
			.groupingKey(prometheusGroupingKey, prometheusJobName)
			.registry(prometheusRegistry)
			.build();
	}

	@Bean
	public ObservationRegistry observationRegistry(PrometheusMeterRegistry meterRegistry) {
		ObservationRegistry observationRegistry = ObservationRegistry.create();
		observationRegistry.observationConfig().observationHandler(new DefaultMeterObservationHandler(meterRegistry));
		return observationRegistry;
	}

}
