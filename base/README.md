# Base Module

Base for other modules and core components.

## Features

| Feature | Why? | Demo |
| --- | --- | --- |
| Easily create infrastructure using TCI<sup>[JD](https://javadoc.io/doc/software.xdev.tci/base/latest/software/xdev/tci/TCI.html)</sup> (TestContainer Infrastructure) templating + Factories for that | Makes writing and designing tests easier | [here](../base-demo/src/test/java/software/xdev/tci/dummyinfra/) |
| [PreStarting mechanism](./src/main/java/software/xdev/tci/factory/prestart/)<sup>[JD](https://javadoc.io/doc/software.xdev.tci/base/latest/software/xdev/tci/factory/prestart/PreStartableTCIFactory.html)</sup> for [additional performance](../PERFORMANCE.md) | Tries to run tests as fast as possible - with a few trade-offs | [here](../base-demo/src/test/java/software/xdev/tci/factory/prestart/) |
| All started containers have a unique human-readable name | Easier identification when tracing or debugging | [here](../base-demo/src/test/java/software/xdev/tci/safestart/) |
| An optimized [implementation of Network](./src/main/java/software/xdev/tci/network/)<sup>[JD](https://javadoc.io/doc/software.xdev.tci/base/latest/software/xdev/tci/network/LazyNetwork.html)</sup> | Addresses various problems of the original implementation to speed up tests | [here](../base-demo/src/test/java/software/xdev/tci/network/) |
| [Safe starting of named containers](./src/main/java/software/xdev/tci/safestart/)<sup>[JD](https://javadoc.io/doc/software.xdev.tci/base/latest/software/xdev/tci/safestart/SafeNamedContainerStarter.html)</sup> | Ensures that a container doesn't enter a crash loop during retried startups | [here](../base-demo/src/test/java/software/xdev/tci/safestart/) |
| [Container Leak detection](./src/main/java/software/xdev/tci/leakdetection/)¹ | Prevents you from running out of resources | [here](../base-demo/src/test/java/software/xdev/tci/leak/) |
| [Tracing](./src/main/java/software/xdev/tci/tracing/)¹ | Makes finding bottlenecks and similar problems easier | |
| [Reporting of fatal (Java) crashes during container startup](./src/main/java/software/xdev/tci/startup/error/java/fatal/) | Easier error diagnosis | [here](../advanced-demo/integration-tests/tci-webapp/src/main/java/software/xdev/tci/demo/tci/webapp/containers/WebAppContainer.java) |
| [Fail-fast/Abortable wait strategies](./src/main/java/software/xdev/tci/startup/wait/) | Fail fast when a container crashes - don't wait until the timeout | [here](../advanced-demo/integration-tests/tci-webapp/src/main/java/software/xdev/tci/demo/tci/webapp/containers/WebAppContainer.java) |

¹ = Active by default due to service loading

## Usage
Take a look at the [minimalistic demo](../base-demo/) that showcases the components individually.

## Config

### PreStart

<details><summary>The configuration is dynamically loaded from (sorted by highest priority)</summary>

* Environment variables 
    * prefixed with `TCI_INFRA-PRE-START_<preStartName>_`*
    * prefixed with `TCI_INFRA-PRE-START_`
    * all properties are in UPPERCASE and use `_` instead of `.` or `-`
* System properties
    * prefixed with `tci.infra-pre-start.<preStartName>.`*
    * prefixed with `tci.infra-pre-start.`

_NOTE: `*` indicates that only some properties support this_

</details>

<details><summary>Full list of configuration options</summary>

| Property | Type | Default | Notes |
| --- | --- | --- | --- |
| `enabled` | `bool` | `false` | Should PreStarting be enabled? |
| `keep-ready`* | `int` | [`junit.jupiter.execution.parallel.`<br/>`config.fixed.max-pool-size`](https://docs.junit.org/6.1.2/writing-tests/parallel-execution.html) or `1` | How many container should be kept ready for use in the background?<br/>Setting this to a value `< 0` will effectively disable PreStarting |
| `max-start-simultan`* | `int` | [`junit.jupiter.execution.parallel.`<br/>`config.fixed.max-pool-size`](https://docs.junit.org/6.1.2/writing-tests/parallel-execution.html) or `1` | Maximum amount of containers that should be started simultaneously<br/>Setting a negative value will remove this limitation |
| `direct-network-attach-if-possible`* | `bool` | `true` | <ul><li><code>true</code> - Directly attaches the container to the network during startup if possible</li><li><code>false</code> - Always performs a network-connect as if PreStarting is active. This is slower, however it emulates PreStarting better and may help with finding bugs.</li></ul> |
| `fixate-exposed-ports-if-required`* | `bool` | `true` | Fixates exposed ports when no direct network attach is possible. This is a workaround for <a href="https://github.com/moby/moby/issues/44137">moby/moby#44137</a>. |
| `coordinator.idle-cpu-percent` | `int` | `40`% | Amount of CPU that needs to be idle to allow PreStarting of containers |
| `coordinator.schedule-period-ms` | `int` | `1000` (1s) | How often PreStarting (one factory) should be tried |
| `detect-ending-tests` | `bool` | `true` | Should PreStarting be stopped when tests are ending? |

_NOTE: Properties marked with `*` can additionally can use the `preStartName` for configuration. Example: `tci.infra-pre-start.my-webapp.`_

</details>

### Leak Detection

<details><summary>The configuration is dynamically loaded from (sorted by highest priority)</summary>

* Environment variables 
    * prefixed with `TCI_LEAK-DETECTION_`
    * all properties are in UPPERCASE and use `_` instead of `.` or `-`
* System properties
    * prefixed with `tci.leak-detection.`

</details>

<details><summary>Full list of configuration options</summary>

| Property | Type | Default | Notes |
| --- | --- | --- | --- |
| `enabled` | `bool` | `false` | Should Leak-Detection be enabled? |
| `stop-timeout-ms` | `int` | ~`20000` (~20s) actual value depends on cpuSlownessFactor | How long to wait until all infrastructure is stopped after tests have ended |

</details>
