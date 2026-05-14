# Quandrix

## English Version

## About this project

Quandrix it's what it is. A backend for a Magic The Gathering singles store, marketplace style. It allows stores and people to sell and publish cards of the aforementioned trading card game. 
The system is divided in the following list of decoupled microservices:


| Component | Port | Description |
|---|---|---|
| eureka-server | 8761 | Registry and discovery of services |
| api-gateway | 8080 | Unique entrypoint to the system |
| ms-auth | 8081 | Authentication, JWT and role gestion |
| ms-users | 8082 | People & Stores profiles |
| ms-catalog | 8083 | MtG cards catalogue (via Scryfall API) |
| ms-listings | 8084 | Listing of singles currently at sale |
| ms-orders | 8085 | Buying orders process |
| ms-payments | 8086 | Payment process |
| ms-transactions | 8087 | Read-only transaction history |
| ms-reviews | 8088 | Reviews and reputation of sellers |
| ms-notifications | 8089 | System notifications |
| ms-reports | 8090 | Reports and stats (ADMIN only) |

## Roles

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

## Testing cycle

- POST /auth/register          → register user
- POST /auth/login             → get JWT
- GET  /catalog/search?name=   → search card
- GET  /catalog/{scryfallId}   → get card via ID
- POST /listings               → add listing
- POST /orders                 → buying and item
- GET  /payments/order/{id}    → verify payment
- POST /reviews                → add review
- GET  /reports/summary        → see reports (ADMIN)

## Licence

Distributed under GPL2 licence.

## Contact

- Ignacia Padilla - svt.nova@pm.me
- Elba Sánchez - elb.sanchezs@duocuc.cl

## Versión en Español

### Acerca del proyecto

Quandrix es lo que es. Un backend para una tienda de venta de cartas al estilo marketplace. Permite que tiendas y personas puedan comprar y publicar cartas singles del juego Magic The Gathering. 
El sistema está dividido en microservicios desacoplados descritos en la siguiente tabla:


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
| ms-transactions | 8087 | Historial inmutable de transacciones |
| ms-reviews | 8088 | Reseñas y reputación de vendedores |
| ms-notifications | 8089 | Notificaciones del sistema |
| ms-reports | 8090 | Informes y estadísticas (solo ADMIN) |

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

### Licencia

Distribuido bajo licencia GPL2.

### Contacto

- Ignacia Padilla - svt.nova@pm.me
- Elba Sánchez - elb.sanchezs@duocuc.cl

