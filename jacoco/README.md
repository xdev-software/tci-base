# JaCoCo

Provides support for [JaCoCo](https://github.com/jacoco/jacoco) Code Coverage in combination with Java containers.
* Allows for recording and extraction of code coverage files from containers
* Support for Testcontainers API

NOTE: Using this is a bit complex as it also requires some changes to the java application in the container.<br/>
Please have a look at the [advanced-demo](../advanced-demo/) and the corresponding [GitHub Actions workflow](../.github/workflows/run-integration-tests.yml) for details.

## Config

<details><summary>The configuration is dynamically loaded from (sorted by highest priority)</summary>

* Environment variables 
    * prefixed with `TCI_JACOCO_`
    * all properties are in UPPERCASE and use `_` instead of `.` or `-`
* System properties
    * prefixed with `tci.jacoco.`

</details>

<details><summary>Full list of configuration options</summary>

| Property | Type | Default | Notes |
| --- | --- | --- | --- |
| `enabled` | `bool` | `false` | You should probably set this to `true` |
| `execution-data-files-dir` | `string` | `target/jacoco-execution-data-files` | Location where the jacoco execution reports/execution data files are stored |
| `move-old-execution-data-files-dir` | `bool` | `true` | If `execution-data-files-dir` already exists it will be moved to `execution-data-files-dir-old` |
| `execution-data-files-dir-old` | `string` | `target/jacoco-execution-data-files-old` | Location where previous jacoco execution reports/execution data files are moved to. If the directory already exists it will be deleted. |
| `execution-data-file-suffix` | `string` | `-jacoco.exec` | Suffix for the jacoco reports/execution data files |

</details>

## Example Report
![](../assets/JaCoCoExample_Overview.avif)
![](../assets/JaCoCoExample_Details.avif)
