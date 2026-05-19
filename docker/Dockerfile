FROM dhi.io/eclipse-temurin:21-alpine3.23-dev AS builder

COPY trustedrootcertificates/ trustedrootcertificates/
RUN for cert in /trustedrootcertificates/*; do echo "yes" | $JAVA_HOME/bin/keytool -import -trustcacerts -file $cert -alias $(basename $cert) -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit; done && \
    rm -rf /trustedrootcertificates/

FROM dhi.io/eclipse-temurin:21-alpine3.23
ENV TZ=Europe/Amsterdam

COPY --from=builder $JAVA_HOME/lib/security/cacerts $JAVA_HOME/lib/security/cacerts

COPY extracted/dependencies ./
COPY extracted/spring-boot-loader ./
COPY extracted/snapshot-dependencies/ ./
COPY extracted/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "-Xmx1024m", "-Dsun.net.inetaddr.ttl=0", "-Djava.security.egd=file:/dev/./urandom", "org.springframework.boot.loader.launch.JarLauncher"]
