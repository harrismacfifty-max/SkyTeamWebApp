FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
ENV PORT=8080
ENV APP_PROFILE=demo

COPY --from=build /build/target/flight-school-backend-0.2.0.jar /app/app.jar

EXPOSE 8080

CMD ["sh", "-c", "if [ -n \"$EXTRA_CLASSPATH\" ]; then exec java -Dserver.port=${PORT:-8080} -cp \"/app/app.jar:$EXTRA_CLASSPATH\" de.skyteam.flightschool.FlightSchoolApplication; else exec java -Dserver.port=${PORT:-8080} -jar /app/app.jar; fi"]
