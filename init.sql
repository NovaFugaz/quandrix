-- ============================================================
--  Quandrix MTG Marketplace — Script de inicialización SQL
--  Crea bases de datos, usuarios y tablas para todos los
--  microservicios. Compatible con MySQL 8+.
--  Hibernate (ddl-auto=update) puede complementar este script,
--  pero este garantiza la estructura inicial explícita.
-- ============================================================

-- ──────────────────────────────────────────────
--  BASES DE DATOS
-- ──────────────────────────────────────────────
CREATE DATABASE IF NOT EXISTS quandrix_auth;
CREATE DATABASE IF NOT EXISTS quandrix_users;
CREATE DATABASE IF NOT EXISTS quandrix_catalog;
CREATE DATABASE IF NOT EXISTS quandrix_listings;
CREATE DATABASE IF NOT EXISTS quandrix_orders;
CREATE DATABASE IF NOT EXISTS quandrix_payments;
CREATE DATABASE IF NOT EXISTS quandrix_transactions;
CREATE DATABASE IF NOT EXISTS quandrix_reviews;
CREATE DATABASE IF NOT EXISTS quandrix_notifications;
CREATE DATABASE IF NOT EXISTS quandrix_reports;

-- ──────────────────────────────────────────────
--  USUARIO Y PERMISOS
-- ──────────────────────────────────────────────
GRANT ALL PRIVILEGES ON quandrix_auth.*         TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_users.*        TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_catalog.*      TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_listings.*     TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_orders.*       TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_payments.*     TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_transactions.* TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_reviews.*      TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_notifications.*TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_reports.*      TO 'quandrix'@'%';
FLUSH PRIVILEGES;

-- ============================================================
--  ms-auth  →  quandrix_auth
-- ============================================================
USE quandrix_auth;

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL,  -- ADMIN | TIENDA | PERSONA
    created_at    DATETIME     NOT NULL,
    PRIMARY KEY (id)
);

-- ============================================================
--  ms-users  →  quandrix_users
-- ============================================================
USE quandrix_users;

CREATE TABLE IF NOT EXISTS user_profiles (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL UNIQUE,  -- FK lógica a ms-auth.users
    display_name VARCHAR(255) NOT NULL,
    created_at   DATETIME     NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS store_profiles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL UNIQUE,   -- FK lógica a ms-auth.users
    store_name  VARCHAR(255),
    location    VARCHAR(255),
    description VARCHAR(500),
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id)
);

-- ============================================================
--  ms-catalog  →  quandrix_catalog
-- ============================================================
USE quandrix_catalog;

CREATE TABLE IF NOT EXISTS card_sets (
    set_code VARCHAR(50)  NOT NULL,
    set_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (set_code)
);

CREATE TABLE IF NOT EXISTS cards (
    scryfall_id VARCHAR(100) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    set_code    VARCHAR(50),
    set_name    VARCHAR(255),
    image_url   VARCHAR(500),
    PRIMARY KEY (scryfall_id),
    CONSTRAINT fk_card_set FOREIGN KEY (set_code)
        REFERENCES card_sets (set_code)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- ============================================================
--  ms-listings  →  quandrix_listings
-- ============================================================
USE quandrix_listings;

CREATE TABLE IF NOT EXISTS listings (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    seller_id   BIGINT      NOT NULL,           -- FK lógica a ms-users
    scryfall_id VARCHAR(100) NOT NULL,           -- FK lógica a ms-catalog
    condition   VARCHAR(50) NOT NULL,
        -- MINT | NEAR_MINT | EXCELLENT | GOOD |
        -- LIGHT_PLAYED | HEAVILY_PLAYED | POOR | DAMAGED
    price       BIGINT      NOT NULL,
    quantity    INT         NOT NULL,
    status      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
        -- ACTIVE | INACTIVE | WITHDRAWN | SOLD
    created_at  DATETIME    NOT NULL,
    PRIMARY KEY (id)
);

-- ============================================================
--  ms-orders  →  quandrix_orders
-- ============================================================
USE quandrix_orders;

CREATE TABLE IF NOT EXISTS orders (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    buyer_id       BIGINT      NOT NULL,    -- FK lógica a ms-users
    listing_id     BIGINT      NOT NULL,    -- FK lógica a ms-listings
    seller_id      BIGINT      NOT NULL,    -- FK lógica a ms-users
    amount         BIGINT      NOT NULL,
    status         VARCHAR(50) NOT NULL DEFAULT 'PENDING',
        -- PENDING | CONFIRMED | COMPLETED | CANCELLED
    payment_method VARCHAR(50),
    created_at     DATETIME    NOT NULL,
    updated_at     DATETIME,
    PRIMARY KEY (id)
);

-- ============================================================
--  ms-payments  →  quandrix_payments
-- ============================================================
USE quandrix_payments;

CREATE TABLE IF NOT EXISTS payments (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    order_id     BIGINT      NOT NULL UNIQUE,   -- FK lógica a ms-orders
    amount       BIGINT      NOT NULL,
    method       VARCHAR(50) NOT NULL,
        -- CREDIT_CARD | DEBIT_CARD | BANK_TRANSFER | CASH
    status       VARCHAR(50) NOT NULL DEFAULT 'PENDING',
        -- PENDING | APPROVED | REJECTED
    processed_at DATETIME,
    created_at   DATETIME    NOT NULL,
    PRIMARY KEY (id)
);

-- ============================================================
--  ms-transactions  →  quandrix_transactions
-- ============================================================
USE quandrix_transactions;

CREATE TABLE IF NOT EXISTS transactions (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    order_id     BIGINT       NOT NULL UNIQUE,  -- FK lógica a ms-orders
    buyer_id     BIGINT       NOT NULL,         -- FK lógica a ms-users
    seller_id    BIGINT       NOT NULL,         -- FK lógica a ms-users
    scryfall_id  VARCHAR(100) NOT NULL,         -- FK lógica a ms-catalog
    amount       BIGINT       NOT NULL,
    completed_at DATETIME     NOT NULL,
    PRIMARY KEY (id)
);

-- ============================================================
--  ms-reviews  →  quandrix_reviews
-- ============================================================
USE quandrix_reviews;

CREATE TABLE IF NOT EXISTS reviews (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    reviewer_id BIGINT NOT NULL,    -- FK lógica a ms-users (comprador)
    seller_id   BIGINT NOT NULL,    -- FK lógica a ms-users (vendedor)
    rating      INT    NOT NULL,    -- 1 a 5
    comment     VARCHAR(500),
    created_at  DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_reviewer_seller (reviewer_id, seller_id)
);

-- ============================================================
--  ms-notifications  →  quandrix_notifications
-- ============================================================
USE quandrix_notifications;

CREATE TABLE IF NOT EXISTS notifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,   -- FK lógica a ms-users
    type       VARCHAR(50)  NOT NULL,
        -- NUEVA_ORDEN | PAGO_CONFIRMADO | PUBLICACION_VENDIDA
        -- | RESEÑA_RECIBIDA | ORDEN_CANCELADA
    message    VARCHAR(500) NOT NULL,
    is_read    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id)
);

-- ============================================================
--  ms-reports  →  quandrix_reports
--  Este microservicio no tiene persistencia propia;
--  consume datos de ms-transactions vía Feign.
--  Se crea la base de datos por consistencia de arquitectura.
-- ============================================================
USE quandrix_reports;
-- (sin tablas propias)
