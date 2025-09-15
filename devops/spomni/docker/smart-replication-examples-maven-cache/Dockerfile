FROM docker.repo-ci.sfera.inno.local/optrn-docker-pub/omni/base_images/astra-1.8.1-openjdk-21-jdk:20250207 AS builder
ARG DOCKER_HOST
ARG DOCKER_CONFIG
ARG CORPDEV_LOGIN
ARG CORPDEV_PASSWORD
ARG ETCD_DOCKER_IMAGE_NAME=docker.repo-ci.sfera.inno.local/tsaj-docker/testcontainers/etcd:3.4.7
COPY ./spomni-config /tmp/spomni-config
COPY ./ /workdir
COPY .testcontainers.properties /root/.testcontainers.properties
RUN mvn -s settings.xml -f pom.xml clean package

FROM docker.repo-ci.sfera.inno.local/optrn-docker-pub/omni/base_images/astra-1.8.1-openjdk-21-jdk:20250207
COPY --from=builder /root/.m2 /root/.m2