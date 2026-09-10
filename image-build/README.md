# Image-Build

Provides common utility and configuration to build images.

## Config

<details><summary>The configuration is dynamically loaded from (sorted by highest priority)</summary>

* Environment variables
    * prefixed with `TCI_IMAGE-BUILD_<imageName>_`
        * where `imageName` is the (sanitized) name of the image to build
    * prefixed with `TCI_IMAGE-BUILD_`
    * all properties are in UPPERCASE and use `_` instead of `.` or `-`
* System properties
    * prefixed with `tci.image-build.<imageName>.`
        * where `imageName` is the (sanitized) name of the image to build
    * prefixed with `tci.image-build.`

_NOTE: Sanitized image-names only include alphanumeric characters, `_` or `-`. All other characters are replaced by `_`_

</details>

<details><summary>Full list of configuration options</summary>

| Property | Type | Default | Notes |
| --- | --- | --- | --- |
| `delete-on-exit` | `bool` | `false` | Should the image be deleted on exit? |
| `logger-for-build-prefix` | `string` | `container.build.` | Prefix used for the build logger |
| `cache-from` | `string` | - | Only applies to BuildKit (native) build.<br/> See [Docker docs](https://docs.docker.com/build/cache/backends/) for details. |
| `cache-to` | `string` | - | Only applies to BuildKit (native) build.<br/> See [Docker docs](https://docs.docker.com/build/cache/backends/) for details. |
| `save-cache-in-background` | `bool` | if `cache-to` set `true`<br/> otherwise `false` | Builds the image the first time WITHOUT `cache-to` and the async in the background again WITH `cache-to`. This way saving the cache does not delay test execution. |
| `wait-for-save-cache-in-background` | `bool` | `true` | Wait until all caches are saved before terminating |

</details>


