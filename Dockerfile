FROM maven:3.9.6-eclipse-temurin-21-alpine as builder

WORKDIR /app
COPY . .

# Compila y lista los JARs generados (debug)
RUN mvn clean package -DskipTests && ls -la target/

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copia CUALQUIER JAR generado en target/
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
