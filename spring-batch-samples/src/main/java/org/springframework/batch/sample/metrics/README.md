## About this sample

This sample shows how to use [Micrometer](https://micrometer.io) to collect batch metrics in Spring Batch.
It uses [Prometheus](https://prometheus.io) as the metrics back end and [Grafana](https://grafana.com) as the front end. 
The sample consists of two jobs:

* `job1` : Composed of two tasklets that print `hello` and `world`
* `job2` : Composed of single chunk-oriented step that reads and writes a random number of items

These two jobs are run repeatedly at regular intervals and might fail randomly for demonstration purposes.

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
