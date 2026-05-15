# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install tectonic
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates && \
    curl -fsSL https://drop.tectonic.ws/install.sh | sh && \
    apt-get remove -y curl && apt-get autoremove -y && rm -rf /var/lib/apt/lists/*

ENV TECTONIC_BIN=/root/.local/bin/tectonic

# Pre-warm tectonic so first PDF compile doesn't download packages at runtime
RUN echo '\documentclass{article}\usepackage[empty]{fullpage}\usepackage{titlesec}\usepackage{marvosym}\usepackage[usenames,dvipsnames]{color}\usepackage{verbatim}\usepackage{enumitem}\usepackage[hidelinks]{hyperref}\usepackage{fancyhdr}\usepackage[english]{babel}\usepackage{tabularx}\usepackage{latexsym}\begin{document}warm\end{document}' \
      > /tmp/warm.tex && \
    $TECTONIC_BIN /tmp/warm.tex && \
    rm -f /tmp/warm.tex /tmp/warm.pdf

COPY --from=build /app/target/resume-pipeline-0.1.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
