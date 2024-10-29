# See also https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html for further information
FROM eclipse-temurin:21-jre-alpine AS builder

WORKDIR /builder

COPY target/webapp.jar app.jar

RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted


FROM eclipse-temurin:21-jre-alpine

ARG ENABLE_CDS=

ARG user=app
ARG group=app
ARG uid=1000
ARG gid=1000
ARG APP_DIR=/opt/app

# Create user + group + home
RUN mkdir -p ${APP_DIR} \
  && chown ${uid}:${gid} ${APP_DIR} \
  && addgroup -g ${gid} ${group} \
  && adduser -h "$APP_DIR" -u ${uid} -G ${group} -s /bin/bash -D ${user}

WORKDIR ${APP_DIR}

USER ${user}

COPY --from=builder --chown=${user}:${group} /builder/extracted/dependencies/ ./
COPY --from=builder --chown=${user}:${group} /builder/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=${user}:${group} /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=${user}:${group} /builder/extracted/application/ ./

# Do CDS and boot with dummy configuration
RUN if [[ ! -z "$ENABLE_CDS" ]]; then \
  java -XX:ArchiveClassesAtExit=app.jsa \
  -Dspring.context.exit=onRefresh \
  # Disable Flyway
  -Dspring.flyway.enabled=false \
  # Disabled DB actuator endpoint
  -Dmanagement.health.db.enabled=false \
  # Disable DB instant connection
  -Dspring.jpa.database-platform=software.xdev.tci.demo.persistence.config.dialect.DefaultDialect \
  -Dspring.jpa.properties.hibernate.boot.allow.jdbc.metadata.access=false \
  # Fake DB
  -Dspring.datasource.url=jdbc:mariadb://localhost:3306/dummy \
  -Dspring.datasource.username=dummy \
  -Dspring.datasource.password=dummy \
  # Fake Auth
  -Dspring.security.oauth2.client.registration.local.authorization-grant-type=authorization_code \
  -Dspring.security.oauth2.client.registration.local.redirect-uri="{baseUrl}/{action}/oauth2/code/{registrationId}" \
  -Dspring.security.oauth2.client.provider.local.authorization-uri=http://localhost/connect/authorize \
  -Dspring.security.oauth2.client.provider.local.token-uri=http://localhost/connect/token \
  -Dspring.security.oauth2.client.provider.local.jwk-set-uri=http://localhost/.well-known/openid-configuration/jwks \
  -Dspring.security.oauth2.client.provider.local.user-info-uri=http://localhost/connect/userinfo \
  -Dspring.security.oauth2.client.provider.local.user-info-authentication-method=header \
  -Dspring.security.oauth2.client.provider.local.user-name-attribute=sub \
  -Dspring.security.oauth2.client.registration.local.client-name=Local \
  -Dspring.security.oauth2.client.registration.local.client-id=dummy \
  -Dspring.security.oauth2.client.registration.local.client-secret=dummy \
  -Dspring.security.oauth2.client.registration.local.scope="openid,profile,email,offline_access" \
  # Actuator
  -Ddemo.actuator.username=dummy \
  -Ddemo.actuator.password-hash=8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918 \
  -jar app.jar; \
  fi

# MaxRAMPercentage: Default value is 25% -> we want to use available memory optimal -> increased, but enough is left for other RAM usages like e.g. Metaspace
# Min/MaxHeapFreeRatio: Default values cause container reserved memory not to shrink properly/waste memory -> decreased
# https://stackoverflow.com/questions/16058250/what-is-the-purpose-of-xxminheapfreeratio-and-xxmaxheapfreeratio
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=30 -Djava.awt.headless=true"
ENV JAVA_CDS_OPTS=${ENABLE_CDS:+"-XX:SharedArchiveFile=app.jsa"}

EXPOSE 8080

CMD [ "/bin/sh", "-c", "java $JAVA_OPTS $JAVA_CDS_OPTS -jar app.jar" ]
