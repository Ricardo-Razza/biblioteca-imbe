# API da biblioteca. Os testes não rodam aqui (precisam do Docker para o Testcontainers): rode
# ./mvnw verify antes de gerar a imagem.
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
# Tira o CR caso o mvnw venha de um checkout no Windows
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests && cp target/biblioteca-*.jar /app.jar

FROM eclipse-temurin:25-jre-noble
ENV TZ=America/Sao_Paulo \
    CAPAS_DIR=/dados/capas \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
RUN groupadd --system biblioteca && useradd --system --gid biblioteca --home-dir /app biblioteca \
    && mkdir -p /app /dados/capas && chown -R biblioteca:biblioteca /app /dados
WORKDIR /app
COPY --from=build /app.jar app.jar
USER biblioteca
VOLUME /dados/capas
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
