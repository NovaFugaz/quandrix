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

GRANT ALL PRIVILEGES ON quandrix_auth.* TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_users.* TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_catalog.* TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_listings.* TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_orders.* TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_payments.* TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_transactions.* TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_reviews.* TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_notifications.* TO 'quandrix'@'%';
GRANT ALL PRIVILEGES ON quandrix_reports.* TO 'quandrix'@'%';

FLUSH PRIVILEGES;