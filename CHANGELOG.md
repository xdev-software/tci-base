# 1.1.3
* Migrated deployment to _Sonatype Maven Central Portal_ [#155](https://github.com/xdev-software/standard-maven-template/issues/155)

# 1.1.2
* Update docs
* Fix ``ContainerMemory#M8G`` being the same as ``ContainerMemory#M4G``

# 1.1.1
* Added ``ContainerMemory`` utility class as it's needed in most projects
* Updated dependencies

# 1.1.0
* Port fixation 
    * can now be disabled
    * now also respects non-TCP ports (e.g. UDP)
    * acquires free ports in batches (previously each port was acquired individually)
* [Demo] Explicit database dialect is no longer required for connection-less start

# 1.0.5
* Updated dependencies
* [Demo] Use [SSE](https://github.com/xdev-software/spring-security-extras) to minimize code

# 1.0.4
* Updated dependencies

# 1.0.3
* Fix ``ConcurrentModificationException`` due to missing synchronized blocks
* Don't warmUp already warmed up factories again on coordinator level
* Correctly register factories to ``GlobalPreStartCoordinator``
* Document ``warmUp``
* Updated dependencies

# 1.0.2
* Fix unlikely modification error "Accept exceeded fixed size of ..." during warm-up
* Updated dependencies

# 1.0.1 
* Make it possible to disable agents
* Improved D&D (docs and demo)

# 1.0.0
_Initial release_
