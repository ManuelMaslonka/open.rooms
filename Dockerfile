FROM eclipse-temurin:24-jdk AS build

WORKDIR /app
COPY . .

RUN ./mvnw clean install

# ===== Stage 2: Run on lightweight JVM =====
FROM eclipse-temurin:24-jre AS runtime

WORKDIR /app
COPY --from=build app/target/open.rooms-0.0.1-SNAPSHOT.jar open.rooms-0.0.1-SNAPSHOT.jar

# Step 1: Extract JAR using jarmode=tools
RUN java -Djarmode=tools -jar open.rooms-0.0.1-SNAPSHOT.jar extract --destination optimized-app

# Step 2: Record AOT configuration
RUN java -XX:AOTMode=record -XX:AOTConfiguration=open.rooms-0.0.1-SNAPSHOT.aotconf -Dspring.context.exit=onRefresh -jar open.rooms-0.0.1-SNAPSHOT.jar

# Step 3: Create AOT cache
RUN java -XX:AOTMode=create -XX:AOTConfiguration=open.rooms-0.0.1-SNAPSHOT.aotconf -XX:AOTCache=open.rooms-0.0.1-SNAPSHOT.aot -jar open.rooms-0.0.1-SNAPSHOT.jar

# Optional: Pass JVM_OPTS as Docker env var
ENV JVM_OPTS=""

# Step 4: Run application with AOT cache
ENTRYPOINT ["sh", "-c", "echo Running with JVM_OPTS: $JVM_OPTS && java $JVM_OPTS -XX:AOTCache=open.rooms-0.0.1-SNAPSHOT.aot -jar open.rooms-0.0.1-SNAPSHOT.jar"]
