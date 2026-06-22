FROM php:8.3-apache

WORKDIR /var/www/html
RUN printf '%s\n' \
    'AddDefaultCharset UTF-8' \
    'AddType "application/javascript; charset=UTF-8" .js' \
    'AddType "text/css; charset=UTF-8" .css' \
    'AddType "text/html; charset=UTF-8" .html' \
    > /etc/apache2/conf-available/skyteam-charset.conf \
    && a2enconf skyteam-charset
COPY . /var/www/html/

EXPOSE 80
