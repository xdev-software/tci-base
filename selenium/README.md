# Selenium

TCI for [Selenium](https://github.com/SeleniumHQ/selenium).

## Features

* All improvements from [xdev-software/testcontainers-selenium](https://github.com/xdev-software/testcontainers-selenium/)
* Predefined browsers (Firefox, Chromium)
* NoVNC support
* Full scale support for recording videos
* Browser Logs can be enabled if required

## Config

<details><summary>The configuration is dynamically loaded from (sorted by highest priority)</summary>

* Environment variables 
    * prefixed with `TCI_SELENIUM_`
    * all properties are in UPPERCASE and use `_` instead of `.` or `-`
* System properties
    * prefixed with `tci.selenium.`

</details>

<details><summary>Full list of configuration options</summary>

| Property | Type | Default | Notes |
| --- | --- | --- | --- |
| `record-mode` | `Enum` | `RECORD_FAILING` | Recording mode.<br/>Available:<ul><li>SKIP - Do not record any videos</li><li>RECORD_ALL - Record all tests</li><li>RECORD_FAILING - Record failing tests only</li></ul> |
| `dir-for-records` | `String` | `target/records` | Directory for storing the recorded videos |
| `vnc-enabled` | `bool` | `false` | Enable [VNC](https://en.wikipedia.org/wiki/VNC) and [NoVNC](https://github.com/novnc/novnc). This is usually only needed during debugging. |
| `deactivate-cdp-if-possible` | `bool` | `true` | Disable Chrome DevTools Protocol (CDP) if possible.<br/>CDP requires additional maven dependencies (e.g. `selenium-devtools-v137)` that are not present by default and will result in a warning. |
| `min-browser-console-log-level` | `Enum` | `ERROR` | Prints out browser console logs. Configures the MINIMUM log level.<br/>Available options:<ul><li>OFF</li><li>ERROR</li><li>WARN</li><li>INFO</li><li>DEBUG</li><li>ALL</li></ul>

</details>
