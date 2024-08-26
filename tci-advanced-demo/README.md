# Advanced TCI Demo

This demo is a reference implementation for TCI.

It represents a Spring Boot Application with a Database and OIDC authentication that [is tested](./webapp-it/) using Selenium.
There are also test that [validate the database migrations and queries](./persistence-it/).

![Overview](../assets/Advanced-Demo-Overview.drawio.svg)

The most interesting project is probably [webapp-it](./webapp-it/) which contains the integration tests for the WebApp.

> [!TIP]
> Pre defined launchers exist for running the app and the integration tests.

## Module naming
* ``tci-*`` are TestContainer Infrastructure projects (used inside the integration tests)
* ``*-it`` are IntegrationTest projects
* All other projects are used for the webapp at compile time

## Features showcase
* PreStarting:<br/> Can be seen when enabling it using ``-Dinfra-pre-start.enabled=1`` or by running the corresponding launchers
* LazyNetwork:<br/> Search for it inside ``BaseTest``. Can also be observed using ``docker network ls`` while running tests.
* Safe starting of named containers:<br/> Is integrated inside ``TCI``. Parts of it only kick in on unexpected container start errors. Can also be observed during tests (with e.g. ``docker stats``) as it names the containers.
* Container leak detection:<br/> Only kicks in when you forget to stop infrastructure after a test. Can be manually reproduced by commenting out the corresponding ``TCI#stop`` methods.
* Tracing:<br/> Seen at the end of each test inside the logs.
