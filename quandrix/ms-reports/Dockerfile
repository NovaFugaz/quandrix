FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar el pom.xml padre
COPY pom.xml .

# Copiar el pom.xml y el código fuente del microservicio
COPY quandrix/ms-reports/pom.xml quandrix/ms-reports/
COPY quandrix/ms-reports/src quandrix/ms-reports/src

# Compilar desde dentro del módulo para que no requiera el resto de los módulos
WORKDIR /app/quandrix/ms-reports
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copiar el JAR generado del módulo correspondiente
COPY --from=build /app/quandrix/ms-reports/target/*.jar app.jar

EXPOSE 8089

ENTRYPOINT ["java", "-jar", "app.jar"]