FROM openjdk:21-jdk

WORKDIR /SillyTavernProxy

COPY SillyTavernProxy.jar ./
COPY application.yml ./

EXPOSE 52006
CMD ["java", "-jar", "SillyTavernProxy.jar"]
