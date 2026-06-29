## Versión en Español

### Enlaces de Descarga

- [Versión Nativa (.jar + .bat)](https://drive.google.com/file/d/1_k_eGVD9PZ3zuils3LHj52pD5YmESzPI/view?usp=sharing)
- [Versión DOCKER (.jar + .bat)](https://drive.google.com/file/d/1hdMvt0krOdhRTFr_MqIm1kxESmdzmXwQ/view?usp=sharing)

- [Versión DOCKER Linux (.sh)](https://drive.google.com/file/d/1WvpCwAVc8RxymdsUzH0ane8yc2Y3HLFn/view?usp=sharing)
- [Versión Nativa Linux (.sh)](https://drive.google.com/file/d/1JkddRe9XBwzTMk-UPARXvZyyZuZ9f-wr/view?usp=sharing)

- [Video demostración evaluación 2](https://drive.google.com/file/d/1yZsia_4ayFE48x2Pzcam-_ry-Dglk2uD/view?usp=drive)
- [Video demostración evaluación 3](...)
- [Distribución de tareas](https://docs.google.com/spreadsheets/d/15T5wIh-iSbPMr31C7P7r6oVz_bJo9_TP-5yKWIl3luU/edit?usp=sharing)

### Objetivo del proyecto

El objetivo de Quandrix es implementar un marketplace distribuido para la gestión de cartas mediante una arquitectura escalable basada en microservicios.

Las principales funcionalidades son:

- Registro y autenticación de usuarios.
- Gestión de perfiles de personas y tiendas.
- Consulta de cartas mediante integración con Scryfall.
- Publicación de cartas en venta.
- Creación de órdenes de compra.
- Procesamiento de pagos.
- Sistema de reseñas y reputación.
- Generación de reportes administrativos.
- Historial de transacciones.

### Acerca del proyecto

Quandrix es lo que es. Un backend para una tienda de venta de cartas al estilo marketplace. Permite que tiendas y personas puedan comprar y publicar cartas singles del juego Magic The Gathering. 
El sistema está dividido en microservicios desacoplados descritos en la siguiente tabla:

### Arquitectura general

| Componente | Puerto | Descripción |
|---|---|---|
| eureka-server | 8761 | Registro y descubrimiento de servicios |
| api-gateway | 8080 | Punto de entrada único al sistema |
| ms-auth | 8081 | Autenticación, JWT y gestión de roles |
| ms-users | 8082 | Perfiles de personas y tiendas |
| ms-catalog | 8083 | Catálogo de cartas MTG (integración Scryfall) |
| ms-listings | 8084 | Publicaciones de singles en venta |
| ms-orders | 8085 | Gestión de órdenes de compra |
| ms-payments | 8086 | Procesamiento de pagos |
| ms-reviews | 8087 | Reseñas y reputación de vendedores |
| ms-notifications | 8088 | Notificaciones del sistema |
| ms-reports | 8089 | Informes y estadísticas (solo ADMIN) |
| ms-transactions | 8090 | Historial inmutable de transacciones |

### Estructura del proyecto

El proyecto está organizado como un proyecto Maven multi-módulo.  
La carpeta principal `quandrix-parent` funciona como proyecto padre y `quandrix` contiene los microservicios independientes junto con la configuración general del sistema.

```text
QUANDRIX-PARENT/
|
└── quandrix/
    |
    ├── pom.xml   (pom.xml padre)
    ├── README.md
    ├── compose.yaml
    ├── LICENSE
    |
    ├── docs/
    |   ├── Documentación - FullStack - proyecto quandrix.pdf    (se necesita extención)
    |   ├── init.sql  (base de datos del proyecto)
    |   └── Quandrix.postman_collection.json
    |
    ├── eureka-server/
    |
    ├── api-gateway/
    |
    ├── ms-auth/
    |
    ├── ms-users/
    |
    ├── ms-catalog/
    |
    ├── ms-listings/
    |
    ├── ms-orders/
    |
    ├── ms-payments/
    |
    ├── ms-reviews/
    |
    ├── ms-notifications/
    |
    ├── ms-reports/
    |
    └── ms-transactions/
```

El script de creación de bases y datos iniciales se encuentra en:

```text
quandrix/docs/init.sql
```

### Requesitos antes de ejecutar

- java 21
- Maven
- Docker + Docker compose
- Extensión vscode (spring boot dashboard)
- XAMPP

### Verificar instalaciones
- java -version
- mvn --v
- docker --version

### Cómo levantar quandrix (DOCKER)

- Asegúrate de tener Docker instalado y su servicio corriendo, además de la dependencia necesaria para compilar, buildx.
- Una vez esté listo eso:
  ```bash
  git clone https://github.com/NovaFugaz/quandrix.git
  docker compose build # puede requerir elevación de privilegios en Linux 
  docker compose up -d # para que corra en segundo plano
  ```
- Para revisar el estado de los servicios:
`docker ps -a` (todo servicio debería decir Up)

### Levantar proyecto con archivos .bat
Visitar los links del principio y elegir el que se acomode versión nativa o Docker.

- Descomprimir carpeta. 
1. En ambos zip se encontraran archivos .bat que permitiran levantar el proyecto, hacer backup de la base de datos, restaurar la base de datos y detener los microservicios.
2. Para una correcta ejecución revisar los requisitos necesarios. (Requesitos antes de ejecutar)

### Roles

- **ADMIN**: acceso total, gestión de usuarios e informes
- **TIENDA**: compra y venta, gestión de listings propios
- **PERSONA**: compra y venta de cartas propias
- **GUEST**: consulta de catálogo sin autenticación

### Hecho con

- Java 21
- Maven
- MySQL
- Spring Boot
- Spring Data JPA + Hibernate
- Spring Security + JWT
- Spring Cloud (Eureka, Gateway, OpenFeign)
- Docker + Docker Compose
- Swagger/OpenApi
- JUnit 5/Mockito


### Flujo principal de prueba
- POST /auth/register          → registrar usuario
- POST /auth/login             → obtener JWT
- GET  /catalog/search?name=   → buscar carta
- GET  /catalog/{scryfallId}   → obtener carta por ID
- POST /listings               → publicar listing
- POST /orders                 → crear orden de compra
- GET  /payments/order/{id}    → verificar pago
- POST /reviews                → dejar reseña
- GET  /reports/summary        → ver resumen (ADMIN)

### Documentación con Swagger
Cada microservicio cuenta con documentación Swagger.

- http://localhost:{puerto}/swagger-ui.html

### Pruebas unitarias

El proyecto incluye pruebas unitarias orientadas a validar la lógica principal de cada microservicio.
Las pruebas están implementadas utilizando:

- JUnit 5
- Mockito

El objetivo de las pruebas es verificar:

- Lógica de negocio de los servicios.
- Validaciones de datos.
- Respuestas esperadas de los componentes principales.
- Manejo de errores.

## Estado de pruebas unitarias

| Microservicio       | Tests Controller | Tests Service | Total | Estado |
|----------------------|:----------------:|:--------------:|:-----:|:------:|
| ms-auth              | 6                 | 8 + 5 (Jwt)     | 19    |   OK   |
| ms-catalog           | 8                 | 10 + 7 (Scryfall) | 25  |   OK   |
| ms-listings          | 18                | 24              | 42    |   OK   |
| ms-notifications     | 13                | 16              | 29    |   OK   |
| ms-reports           | 9                 | 12              | 21    |   OK   |
| ms-orders            | 13                | 18              | 31    |   OK   |
| ms-payments          | 7                 | 7               | 14    |   OK   |
| ms-reviews           | 13                | 15              | 28    |   OK   |
| ms-transactions      | 13                | 10              | 23    |   OK   |
| ms-users             | 20                | 18              | 38    |   OK   |
| **Total**            |                   |                 | **270** |   OK   |

> Todos los tests fueron ejecutados vía `mvn test` en cada módulo, sin fallas ni errores (`Failures: 0, Errors: 0`).

### Licencia

Distribuido bajo licencia GPL2.

### Contacto

- Ignacia Padilla - svt.nova@pm.me
- Elba Sánchez - elb.sanchezs@duocuc.cl


## English Version

### Download links

- [Native version (.jar + .bat)](https://drive.google.com/file/d/1TddXEAvHPBh276X3h0RdY6OtWUPe0GJX/view?usp=sharing)
- [DOCKER version (.jar + .bat)](https://drive.google.com/file/d/1hdMvt0krOdhRTFr_MqIm1kxESmdzmXwQ/view?usp=sharing)

- [DOCKER version linux (.sh)](https://drive.google.com/file/d/1WvpCwAVc8RxymdsUzH0ane8yc2Y3HLFn/view?usp=sharing)
- [Native version Linux (.sh)](https://drive.google.com/file/d/1JkddRe9XBwzTMk-UPARXvZyyZuZ9f-wr/view?usp=sharing)


- [Video demonstration evaluation 2](https://drive.google.com/file/d/1yZsia_4ayFE48x2Pzcam-_ry-Dglk2uD/view?usp=drive)
- [Video demonstration evaluation 3](...)
- [Task distribution](https://docs.google.com/spreadsheets/d/15T5wIh-iSbPMr31C7P7r6oVz_bJo9_TP-5yKWIl3luU/edit?usp=sharing)

### Project Objective

The objective of Quandrix is to implement a distributed marketplace for card management using a scalable, microservices-based architecture.

The main functionalities are:

- User registration and authentication.
- Management of user store profiles.
- Card querying via integration with Scryfall.
- Listing cards for sale.
- Creation of purchase orders.
- Payment processing.
- Review and reputation system.
- Generation of administrative reports.


## About this project

Quandrix is a backend for a Magic The Gathering singles store, marketplace style. It allows stores and people to sell and publish cards of the aforementioned trading card game. 
The system is divided in the following list of decoupled microservices:


| Component | Port | Description |
|---|---|---|
| eureka-server | 8761 | Registry and discovery of services |
| api-gateway | 8080 | Unique entrypoint to the system |
| ms-auth | 8081 | Authentication, JWT and role management |
| ms-users | 8082 | People & Stores profiles |
| ms-catalog | 8083 | MtG cards catalogue (via Scryfall API) |
| ms-listings | 8084 | Listing of singles currently at sale |
| ms-orders | 8085 | Buying orders process |
| ms-payments | 8086 | Payment process |
| ms-reviews | 8087 | Reviews and reputation of sellers |
| ms-notifications | 8088 | System notifications |
| ms-reports | 8089 | Reports and stats (ADMIN only) |
| ms-transactions | 8090 | Read-only transaction history |

### Project Structure

The project is organized as a multi-module Maven project.  
The top-level folder `quandrix-parent` serves as the parent project, and `quandrix` contains the independent microservices along with the general system configuration.

```text
QUANDRIX-PARENT/
|
└── quandrix/
    |
    ├── pom.xml   (pom.xml parent)
    ├── README.md
    ├── compose.yaml
    ├── LICENSE
    |
    ├── docs/
    |   ├── Documentación - FullStack - proyecto quandrix.pdf    (An extension is needed - vscode pdf)
    |   ├── init.sql  (project database)
    |   └── Quandrix.postman_collection.json
    |
    ├── eureka-server/
    |
    ├── api-gateway/
    |
    ├── ms-auth/
    |
    ├── ms-users/
    |
    ├── ms-catalog/
    |
    ├── ms-listings/
    |
    ├── ms-orders/
    |
    ├── ms-payments/
    |
    ├── ms-reviews/
    |
    ├── ms-notifications/
    |
    ├── ms-reports/
    |
    └── ms-transactions/
```

The script for creating databases and initial data is located at:

```text
quandrix/docs/init.sql
```

### Before running the project

- Java 21
- Maven
- Docker + Docker Compose
- VS Code extension (Spring Boot Dashboard)

### Verify installations
- java -version
- mvn --v
- docker --version

### Launching the project with .bat files
Visit the links at the beginning and choose the one that suits you: native version or Docker.

- Unzip the folder. 
1. Both ZIP files contain .bat files that allow you to launch the project, back up the database, restore the database, and stop the microservices.
2. To ensure proper execution, review the necessary requirements. (Before running the project)

### How to Set Up the Quadrix

- Make sure to have docker installed and running. You may need docker buildx dependency to build it.
- Once you are ready:
  ```bash
  git clone https://github.com/NovaFugaz/quandrix.git
  docker compose build # you may need to run it through sudo/doas
  docker compose up -d # so it runs on background
  ```
- To check the health of all microservices:
`docker ps -a` (every container should say Up)

### Roles

- **ADMIN**: full access, user and reports management
- **TIENDA**: buying and selling own cards, listing management
- **PERSONA**: buying and selling own cards
- **GUEST**: can only see the catalogue

### Built with

- Java 21
- Maven
- MySQL
- Spring Boot
- Spring Data JPA + Hibernate
- Spring Security + JWT
- Spring Cloud (Eureka, Gateway, OpenFeign)
- Docker + Docker Compose
- Swagger/OpenApi
- JUnit 5/Mockito

## Testing cycle

- POST /auth/register          → register user
- POST /auth/login             → get JWT
- GET  /catalog/search?name=   → search card
- GET  /catalog/{scryfallId}   → get card via ID
- POST /listings               → add listing
- POST /orders                 → buying an item
- GET  /payments/order/{id}    → verify payment
- POST /reviews                → add review
- GET  /reports/summary        → see reports (ADMIN)

### Swagger Documentation
Each microservice has Swagger documentation.

- http://localhost:{port}/swagger-ui.html

### Unit Tests

The project includes unit tests designed to validate the core logic of each microservice.
The tests are implemented using:

- JUnit 5
- Mockito

The purpose of the tests is to verify:

- The business logic of the services.
- Data validations.
- Expected responses from the main components.
- Error handling.

## Unit tests summary

| Microservice     | Tests Controller | Tests Service | Total | Status |
|------------------|:----------------:|:--------------:|:-----:|:------:|
| ms-auth          | 6                 | 8 + 5 (Jwt)     | 19    |   OK   |
| ms-catalog       | 8                 | 10 + 7 (Scryfall) | 25  |   OK   |
| ms-listings      | 18                | 24              | 42    |   OK   |
| ms-notifications | 13                | 16              | 29    |   OK   |
| ms-reports       | 9                 | 12              | 21    |   OK   |
| ms-orders        | 13                | 18              | 31    |   OK   |
| ms-payments      | 7                 | 7               | 14    |   OK   |
| ms-reviews       | 13                | 15              | 28    |   OK   |
| ms-transactions  | 13                | 10              | 23    |   OK   |
| ms-users         | 20                | 18              | 38    |   OK   |
| **Total**        |                   |                 | **270** |   OK   |

> All the tests were run through `mvn test`, and there were no failures nor errors. (`Failures: 0, Errors: 0`).

## License

Distributed under GPL2 licence.

## Contact

- Ignacia Padilla - svt.nova@proton.me
- Elba Sánchez - elb.sanchezs@duocuc.cl
