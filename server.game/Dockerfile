# Needs `./gradlew :server.game:installDist` to have run first.
FROM eclipse-temurin:25-jre-noble

RUN groupadd --gid 10001 pirated \
    && useradd --no-log-init --uid 10001 --gid pirated --home-dir /opt/pirated \
       --shell /usr/sbin/nologin pirated

WORKDIR /opt/pirated
COPY server.game/build/install/server.game/lib/ lib/
COPY --chmod=755 server.game/build/install/server.game/bin/ bin/

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

USER 10001:10001
EXPOSE 7777
ENTRYPOINT ["/opt/pirated/bin/server.game"]
