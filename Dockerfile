# Despliegue del backend en Render.
#
# Render no tiene runtime nativo para Java: su documentación solo lista Node,
# Python, Ruby, Go, Rust y Elixir, y para Java/Kotlin/Scala indica usar un
# contenedor. Este archivo es ese contenedor.
#
# Son dos etapas. La primera compila con Maven y pesa mucho; la segunda se lleva
# solo el JAR resultante sobre una imagen de Java sin nada más. Así la imagen
# final ronda 250 MB en vez de los más de 900 MB que tendría si se mandara el
# compilador dentro.
#
# Para construirlas y probarlas en local:
#   docker build -t sih-inventarios-backend .
#   docker run --rm -p 8080:8080 -e DB_PASSWORD=... sih-inventarios-backend

# ============================================================
# Etapa 1: compilar
# ============================================================
# Java 21 (LTS). El pom pide 17, así que el JAR generado también corre en 17;
# 21 es solo el JDK con el que se compila y se ejecuta.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Se copia el pom.xml solo, separado del código. Mientras el pom no cambie,
# Maven reutiliza esta capa con todas las dependencias ya descargadas y el
# siguiente build tarda segundos en lugar de minutos. En cuanto se toque un
# .java, esa capa se conserva y solo se recompila el código.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src

# -DskipTests deja correr la construcción sin las pruebas: en un despliegue lo
# que importa es que compile y arranque. La base de datos de Aiven tampoco
# estaría disponible durante el build.
RUN mvn -B -q -DskipTests clean package

# ============================================================
# Etapa 2: imagen final
# ============================================================
FROM eclipse-temurin:21-jre

# Usuario sin privilegios. La imagen oficial corre como root y en Render no
# hace falta: la aplicación solo lee un archivo y escucha en un puerto.
RUN useradd --system --create-home --uid 1001 spring

WORKDIR /app

# El comodín evita tener que tocar este archivo cada vez que cambie la versión
# del pom. No coge el jar.original que deja spring-boot:repackage, porque ese
# nombre no termina en .jar.
COPY --from=build /app/target/*.jar app.jar

RUN chown spring:spring app.jar
USER spring:spring

# Solo informativo: Docker no publica puertos por este comando. Render asigna
# el puerto con la variable PORT y la aplicación ya la lee
# (server.port=${PORT:8080} en application.properties).
EXPOSE 8080

# -XX:MaxRAMPercentage deja que la JVM dedique a la pila el 75% de la memoria
# que le da el contenedor, en vez de un valor fijo que en un plan gratuito
# pequeño dejaría al proceso sin aire.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]