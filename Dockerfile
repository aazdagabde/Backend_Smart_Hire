# ----- ÉTAPE 1: Construction (Build) -----
# Utilise l'image Maven basée sur Java 17 (défini dans votre pom.xml)
FROM maven:3.9-eclipse-temurin-17 AS build

# Définit le répertoire de travail dans le conteneur
WORKDIR /app

# Copie le fichier pom.xml et les scripts du wrapper maven
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw* .

# Télécharge les dépendances (cela crée une couche de cache)
RUN mvn dependency:go-offline

# Copie le reste du code source
COPY src/ src/

# Construit l'application et crée le .jar
# -DskipTests accélère la construction en sautant les tests
RUN mvn package -DskipTests

# ----- ÉTAPE 2: Exécution (Run) -----
# Utilise une image JRE (Java Runtime) légère basée sur Java 17
FROM eclipse-temurin:17-jre-focal

# Définit le répertoire de travail
WORKDIR /app

# Copie le .jar construit depuis l'étape 'build'
# (Basé sur l'artifactId "api" et la version "0.0.1-SNAPSHOT" de votre pom.xml)
COPY --from=build /app/target/api-0.0.1-SNAPSHOT.jar /app/app.jar

# Expose le port 8080 (le port par défaut de Spring Boot)
EXPOSE 8080

# Commande pour démarrer l'application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]