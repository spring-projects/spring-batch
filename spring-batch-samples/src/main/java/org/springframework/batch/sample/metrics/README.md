## About this sample

This sample shows how to use [Micrometer](https://micrometer.io) to collect batch metrics in Spring Batch.
It uses [Prometheus](https://prometheus.io) as metrics backend and [Grafana](https://grafana.com) as frontend. 
The sample consists of two jobs:

* `job1` : composed of two tasklets that print `hello` and `world`
* `job2` : composed of single chunk-oriented step that reads and writes a random number of items

These two jobs are run repeatedly at regular intervals and might fail randomly for demonstration purpose.

## How to run the sample?

This sample requires [docker compose](https://docs.docker.com/compose/) to start the monitoring stack.
To run the sample, please follow these steps:

```
$>cd spring-batch-samples/src/grafana
$>docker-compose up
```

This should start the required monitoring stack:

* Prometheus server on port `9090`
* Prometheus push gateway on port `9091`
* Grafana on port `3000`

Once started, you need to configure Prometheus as data source in Grafana and import
the ready-to-use dashboard in `spring-batch-samples/src/grafana/spring-batch-dashboard.json`.

Finally, run the `org.springframework.batch.sample.metrics.BatchMetricsApplication`
class without any argument to start the sample.
