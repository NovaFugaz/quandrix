FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar el pom.xml padre
COPY pom.xml .

# Copiar el pom.xml y el código fuente del microservicio
COPY quandrix/ms-notifications/pom.xml quandrix/ms-notifications/
COPY quandrix/ms-notifications/src quandrix/ms-notifications/src

# Compilar desde dentro del módulo para que no requiera el resto de los módulos
WORKDIR /app/quandrix/ms-notifications
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copiar el JAR generado del módulo correspondiente
COPY --from=build /app/quandrix/ms-notifications/target/*.jar app.jar

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "app.jar"]