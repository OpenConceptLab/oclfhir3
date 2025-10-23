# ---------- Stage 1: Build ----------
FROM maven:3.9.9-eclipse-temurin-11 AS build

WORKDIR /usr/src/app

# Copia pom.xml e baixa dependências primeiro para otimizar cache
COPY pom.xml . 
COPY src ./src

# Gera o jar Spring Boot empacotado
RUN mvn clean package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:11-jre

# Cria usuário não-root
RUN useradd -ms /bin/bash gointerop

WORKDIR /app

# Copia só o jar gerado
COPY --from=build /usr/src/app/target/tx-fhir-ocl-*.jar /app/app.jar

# Define permissões
USER gointerop

EXPOSE 80

# Use ENTRYPOINT ou CMD para executar o Spring Boot
CMD ["java", "-jar", "/app/app.jar"]
