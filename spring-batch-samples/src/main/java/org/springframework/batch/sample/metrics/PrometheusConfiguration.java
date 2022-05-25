/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.batch.sample.metrics;

import java.util.HashMap;
import java.util.Map;
import jakarta.annotation.PostConstruct;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class PrometheusConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusConfiguration.class);

	@Value("${prometheus.job.name}")
	private String prometheusJobName;

	@Value("${prometheus.grouping.key}")
	private String prometheusGroupingKey;

	@Value("${prometheus.pushgateway.url}")
	private String prometheusPushGatewayUrl;

	private Map<String, String> groupingKey = new HashMap<>();

	private PushGateway pushGateway;

	private CollectorRegistry collectorRegistry;

	@PostConstruct
	public void init() {
		pushGateway = new PushGateway(prometheusPushGatewayUrl);
		groupingKey.put(prometheusGroupingKey, prometheusJobName);
		PrometheusMeterRegistry prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
		collectorRegistry = prometheusMeterRegistry.getPrometheusRegistry();
		Metrics.globalRegistry.add(prometheusMeterRegistry);
	}

	@Scheduled(fixedRateString = "${prometheus.push.rate}")
	public void pushMetrics() {
		try {
			pushGateway.pushAdd(collectorRegistry, prometheusJobName, groupingKey);
		}
		catch (Throwable ex) {
			LOGGER.error("Unable to push metrics to Prometheus Push Gateway", ex);
		}
	}

}
