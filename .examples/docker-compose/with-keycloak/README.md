## Mit Keycloak / OpenID Connect

Dieses Beispiel zeigt, wie die Urlaubsverwaltung mit [Keycloak](https://www.keycloak.org/)
bzw. einem OpenID Connect fähigen Autorisierungsserver verwendet werden kann.

* In diesem Verzeichnis `docker-compose up -d`
* Vom Root-Verzeichnis `java -jar target/urlaubsverwaltung-*.jar \
  --spring.config.location=file:./src/main/resources/ \
  --spring.profiles.active=demodata,keycloak`


Logins sind auf Basis von [Demodata](../../../README.md#Demodaten-Modus). 
