# Needs `./gradlew :server.login:installDist` to have run first.
FROM eclipse-temurin:25-jre-noble

RUN groupadd --gid 10001 pirated \
    && useradd --no-log-init --uid 10001 --gid pirated --home-dir /opt/pirated \
       --shell /usr/sbin/nologin pirated

WORKDIR /opt/pirated
COPY server.login/build/install/server.login/lib/ lib/
COPY --chmod=755 server.login/build/install/server.login/bin/ bin/

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

USER 10001:10001
EXPOSE 2106
ENTRYPOINT ["/opt/pirated/bin/server.login"]
