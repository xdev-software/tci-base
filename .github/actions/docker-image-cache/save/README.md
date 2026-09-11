Saves the pulled images into the cache

An image will be ignored if:
* it was preinstalled (this information is acquired from [restore](../restore/))
* it is labeled with
  * `org.testcontainers.sessionId` (Testcontainers `deleteOnExit` label)
  * `tci.image-cache.ignore`
