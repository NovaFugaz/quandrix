FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar el pom.xml padre
COPY pom.xml .

# Copiar el pom.xml y el código fuente del microservicio
COPY quandrix/ms-listings/pom.xml quandrix/ms-listings/
COPY quandrix/ms-listings/src quandrix/ms-listings/src

# Compilar desde dentro del módulo para que no requiera el resto de los módulos
WORKDIR /app/quandrix/ms-listings
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copiar el JAR generado del módulo correspondiente
COPY --from=build /app/quandrix/ms-listings/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]