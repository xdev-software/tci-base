# Note: This Dockerfile is used by the selenium tests for compiling the WebApp when there was no Image supplied

FROM maven:3-eclipse-temurin-21-alpine AS build-env

RUN apk add --no-cache git

# Create non root user
ARG userName=limitedbuild
ARG groupName=limitedbuild
ARG userId=1000

RUN addgroup --system ${groupName} \
	&& adduser --uid ${userId} --system --disabled-password --shell /bin/bash ${userName} \
	&& adduser ${userName} ${groupName}

# Create build dir
RUN mkdir /build \
    && chown ${userName}:${groupName} /build
WORKDIR /build

USER ${userName}

# Copying is prepared by TCI
COPY --chown=${userName}:${groupName} . ./

# A valid Git repo is required for the build
RUN git config --global user.email "dynamic@build.local" \
  && git config --global user.name "Dynamic Build" \
  && git init --initial-branch=dynamically-built-tcst \
  && git add . \
  && git commit -m "Init commit"

ARG mavenbuildcmd='mvn -B clean package -pl "webapp" -am -Pprod,dev-log -T2C -Dmaven.test.skip'

RUN echo "Executing '$mavenbuildcmd'"
RUN ${mavenbuildcmd}

FROM eclipse-temurin:22-jre-alpine

ARG user=app
ARG group=app
ARG uid=1000
ARG gid=1000
ARG APP_DIR=/opt/app

# Create user + group + home
RUN mkdir -p ${APP_DIR} \
  && chown ${uid}:${gid} ${APP_DIR} \
  && addgroup -g ${gid} ${group} \
  && adduser -h "$APP_DIR" -u ${uid} -G ${group} -s /bin/sh -D ${user}

# MinRAMPercentage: Default value is 50% -> may reserve memory that it doesn't need -> decreased
# MaxRAMPercentage: Default value is 25% -> we want to use available memory optimal -> increased, but
# enough is left for other RAM usages like e.g. Metaspace
# Min/MaxHeapFreeRatio: Default values cause container reserved memory not to shrink properly/waste memory -> decreased
# https://stackoverflow.com/questions/16058250/what-is-the-purpose-of-xxminheapfreeratio-and-xxmaxheapfreeratio
ENV JAVA_OPTS "-XX:MinRAMPercentage=20 -XX:MaxRAMPercentage=75 -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=30 -Djava.awt.headless=true"

EXPOSE 8080

USER ${user}

COPY --from=build-env --chown=${user}:${group} build/webapp/target/webapp.jar ${APP_DIR}/webapp.jar

CMD java ${JAVA_OPTS} -jar /opt/app/webapp.jar
