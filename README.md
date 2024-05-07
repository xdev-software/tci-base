[![Latest version](https://img.shields.io/maven-central/v/software.xdev/tci-base?logo=apache%20maven)](https://mvnrepository.com/artifact/software.xdev/tci-base)
[![Build](https://img.shields.io/github/actions/workflow/status/xdev-software/tci-base/checkBuild.yml?branch=develop)](https://github.com/xdev-software/tci-base/actions/workflows/checkBuild.yml?query=branch%3Adevelop)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=xdev-software_tci-base&metric=alert_status)](https://sonarcloud.io/dashboard?id=xdev-software_tci-base)
[![javadoc](https://javadoc.io/badge2/software.xdev/tci-base/javadoc.svg)](https://javadoc.io/doc/software.xdev/tci-base) 

# <img src="./assets/logo.png" height=28 > Testcontainers Infrastructure (TCI) Framework Base

Basis Module for XDEV's Testcontainer Infrastructure Framework

## Features
| Feature | Why? |
| --- | --- |
| Easily create infrastructure using TCI (TestContainer Infrastructure) templating + Factories for that | Makes writing and designing tests easier |
| [PreStarting mechanism](./tci-base/src/main/java/software/xdev/tci/factory/prestart/) for [additional performance](./PERFORMANCE.md) | Tries to run tests as fast as possible - with a few trade-offs |
| All started containers have a unique human-readable name | Easier identification when tracing or debugging |
| An optimized [implementation of Network](./tci-base/src/main/java/software/xdev/tci/network/) | Addresses various problems of the original implementation to speed up tests |
| [Safe starting of named containers](./tci-base/src/main/java/software/xdev/tci/safestart/) | Ensures that a container doesn't enter a crash loop during retried startups |
| [Container Leak detection](./tci-base/src/main/java/software/xdev/tci/leakdetection/) | Prevents you from running out of resources |
| [Tracing](./tci-base/src/main/java/software/xdev/tci/tracing/) | Makes finding bottlenecks and similar problems easier |

## Usage
Checkout the [advanced demo](./tci-advanced-demo/) - as this is a reference implementation of the features - to get a feeling how this can be done.

> [!TIP]
> More detailed documentation is usually available in the corresponding [JavaDocs](https://javadoc.io/doc/software.xdev/tci-base).

## Installation
[Installation guide for the latest release](https://github.com/xdev-software/tci-base/releases/latest#Installation)

## Support
If you need support as soon as possible and you can't wait for any pull request, feel free to use [our support](https://xdev.software/en/services/support).

## Contributing
See the [contributing guide](./CONTRIBUTING.md) for detailed instructions on how to get started with our project.

## Dependencies and Licenses
View the [license of the current project](LICENSE) or the [summary including all dependencies](https://xdev-software.github.io/tci-base/dependencies)
