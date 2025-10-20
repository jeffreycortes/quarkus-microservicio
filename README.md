# certificados-manager

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/certificados-manager-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Related Guides

- RESTEasy Classic ([guide](https://quarkus.io/guides/resteasy)): REST endpoint framework implementing Jakarta REST and more

## Provided Code

### RESTEasy JAX-RS

Easily start your RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started#the-jax-rs-resources)


# quarkus-microservicio
Ejemplo de microservicio con quarkus


# Pre-requisitos
- Java 17
- Maven
- intelliJ o editor de su preferencia

# 1. Inicializacion
- Crear un nuevo proyecto: ```mvn io.quarkus.platform:quarkus-maven-plugin:3.2.0.Final:create -DprojectGroupId=com.dian -DprojectArtifactId=certificados-manager -Dextensions="resteasy-jackson,quarkus-resteasy"```
- Servir Api: ```./mvnw quarkus:dev```
- Probar endpoint hello: http://localhost:8080/hello

# 2. Primer Microservicio
## 2.1 Proyecto base
- Se crea la clase CertificadosResource con 2 paths (endpoints) que retornan un mensaje de vida con la versión y un mensaje estático.
- Se preestablece el formato json del recurso
- Se implementa ApiRESTFull
- Se implementa SolicitudesCertificadosResource con paths que incluye:
  - Paths con parametros de uri
  - QueryParams opcionales, obligatorios, valores por defecto
  - Bean de QueryString (estructura DTO)
  - Validación de headers
- Paths:
  - ```curl --location 'http://localhost:8080/solicitudes/certificados/123?incluirDetalles=true&formato=json&filtro=activo&pagina=1&tama%C3%B1o=20' --header 'Authorization: Bearer 123456789' --header 'Origin: http://muisca.dian.gov.co'```
  - ```curl --location --request POST 'http://localhost:8080/solicitudes/certificados/1/propio?nombre=01234456.pdf&precio=3500.00&cantidad=1' --header 'Authorization: Bearer 123456789' --header 'Origin: http://muisca.dian.gov.co'```
  - ```curl --location 'http://localhost:8080/solicitudes/reportes?tipo=detallado&fechaInicio=2024-01-01&fechaFin=2024-01-31&limite=200' --header 'Authorization: Bearer 123456789' --header 'Origin: http://muisca.dian.gov.co'```
- Comando para depurar proyecto: ```./mvnw quarkus:dev -Ddebug```. En intelliJ se debe agregar en configuración de depuración. Ver capturas de pantalla anexas de punto 2.
## 2.2 configuracion, contenerizacion y constucción con variables de entorno
- Se agrega la dependencia: quarkus-config-yaml para uso de yml en vez de .propperties como archivo de configuración
- Se elimina application.properties
- Se crea el archivo application.yml
- Se crean archivos de perfiles de configuración develop y release
- Se crea EnvironmentService mapeando los valores del application
- Se crea EnvironmentResource para exponer los valores de entorno heredados del application.yml
- Se crea docker-compose.yml con la configuracion para la construcción del microservicio usando la imagen para jvm
- Compilación: ```./mvnw package```
- Contrucción y despliegue de imagen en contenedor docker: ```docker-compose -f src/main/docker/docker-compose.yml --project-directory . up --build -d``` (Ejecutar en raíz de proyecto)
- Despliegue de imagen (sin construcción o build): ```docker-compose -f src/main/docker/docker-compose.yml --project-directory . up -d``` (Ejecutar en raíz de proyecto)
- Verificar los valores de configuración tanto perfil develop como release: ```curl --location 'http://localhost:8080/env/all'```
- Destrucción de contenedores creados: ``` docker-compose -f src/main/docker/docker-compose.yml --project-directory . down``` (Ejecutar en raíz de proyecto)


# 3. Persistencia de Datos
## 3.1 Configuración de BD
- Creación de archivo .env en la raíz del proyecto y agregar las siguientes variables de entorno para la configuración de la BD Mysql y la conexión co JDBC:
```
# Variables para el sevicio MySQL del docker-compose que la imagen usa de manera estándar 
MYSQL_ROOT_PASSWORD=root_dev_demo_dian
MYSQL_DATABASE=certificados
MYSQL_USER=usr_demo_dian
MYSQL_PASSWORD=pass_demo_dian

# Variables del servicio App del docker-compose que serán usadas para el microservicio
QUARKUS_PROFILE=develop
APP_SOBRENOMBRE="Certificados manager service"
DB_HOST=mysql
DB_PORT=3306
DB_NAME=certificados
DB_USER=root
DB_PASSWORD=root_dev_demo_dian
```
- Se habilita servicio mysql en docker-compose
- Se agrega uso del archivo .env para el microservicio en el docker-compose (servicio app)
- Se establece dependencia entre el servicio app y mysql en el docker-compose
- Ejecutar docker-compose para que despliegue la BD sin el app: - Despliegue de imagen (sin construcción o build): ```docker-compose -f src/main/docker/docker-compose.yml --project-directory . up -d mysql```
- Verificar que la BD esté disponible: ```docker exec -it quarkus-microservicio-mysql-1 mysql -u root -proot_dev_demo_dian -e "SHOW DATABASES;"``` -> En el ejemplo el nombre representa el nombre del contenedor, también puede usarse el id del contenedor.
- Verificar los valores de configuración (variables de entorno)