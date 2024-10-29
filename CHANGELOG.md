# 1.0.3
* Fix ``ConcurrentModificationException`` due to missing synchronized blocks
* Don't warmUp already warmed up factories again on coordinator level
* Correctly register factories to ``GlobalPreStartCoordinator``
* Document ``warmUp``
* Optimized Dockerfiles in demo according to [Spring Boot recommendations](https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html)
* Updated dependencies

# 1.0.2
* Fix unlikely modification error "Accept exceeded fixed size of ..." during warm-up
* Updated dependencies

# 1.0.1 
* Make it possible to disable agents
* Improved D&D (docs and demo)

# 1.0.0
_Initial release_
