# kafka-ops

![Tests](https://github.com/rsolci/kafka-ops/actions/workflows/gradle.yml/badge.svg) [![Maintainability](https://api.codeclimate.com/v1/badges/d7ae667d8ac5639c8d1b/maintainability)](https://codeclimate.com/github/rsolci/kafka-ops/maintainability) [![Test Coverage](https://api.codeclimate.com/v1/badges/d7ae667d8ac5639c8d1b/test_coverage)](https://codeclimate.com/github/rsolci/kafka-ops/test_coverage)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

A simple tool to manage your topics through a simple schema file.

## Overview

Kafka Ops is a simple and lightweight tool to help you automate the management of you Apache Kafka (or API compliant)
cluster.

This tool, as Terraform and other similar infrastructure-as-code tools, uses a schema file to build the desired state of
your cluster.
By scanning you cluster and making a difference it can generate a plan with the required changes to be applied.

## Features

- **Simple schema file**: Declarative, simple to read and version controlled schema file.
- **Easy to read plan**: Plan and apply output changes easily readable.
- **Idempotency**: The same schema file will not change an up-to-date cluster if executed more than once.
- **Non-intrusive**: Can be configured to ignore some topics by name or prefix, allowing this tool to be used on
  existing clusters
- **Non-delete mode**: By default, this tool don't delete any topic, you have to explicitly allow it to do so.

## Usage

Running the tool without any argument will output a help page:

```
Usage: main [OPTIONS] COMMAND [ARGS]...

Options:
  -s, --schema PATH   Schema file to sync changes from
  -d, --allow-delete  Allow the tool to remove resources not found on schema
                      file
  -v, --verbose       Show application and kafka library logs
  -h, --help          Show this message and exit

Commands:
  plan   Plan the execution based on the provided schema file
  apply  Apply the changes based on the provided schema file
```

### Parameters

Kafka Ops get all the required parameters to connect and authenticate to your cluster through environment variables,
following Apache Kafka property name convention prefixed with `KAFKA_`.

For example, to generate an execution plan on a cluster running locally:

* Set the environment variable `KAFKA_BOOTSTRAP_SERVERS` as `localhost:9092`
* Run the tool pointing to a valid schema file: `java -jar kafka-ops.jar -s /path/to/schema.yaml plan`

If your cluster requires authentication, we read the credentials from additional environment variables:

* `KAFKA_USERNAME`: Username to log in to the cluster
* `KAFKA_PASSWORD`: Password to authenticate said user

Both of these options requires the SASL Mechanism and Security Protocol to be set in order to work, following the Apache
Kafka property convention, this configuration can be set by exporting `KAFKA_SASL_MECHANISM`
and `KAFKA_SECURITY_PROTOCOL` environment variables.

## Schema File

The desired schema file is defined using YAML and follows this structure:

```yaml
topics:
  example-topic:
    partitions: 2
    replication: 4
    config:
      retention.ms: 172800000

settings:
  topics:
    ignoreList:
      - dont-touch-topic
      - other-team-prefix*
```

## License

Copyright (c) 2022 Rafael Solci.

Licensed under the [Apache 2.0 license][license].

[license]: ./LICENSE