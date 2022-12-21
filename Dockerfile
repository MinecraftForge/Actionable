# Container image that runs your code
FROM amazoncorretto:17.0.5

COPY dist/app.jar /app.jar

COPY run.sh /run.sh
RUN chmod +x /run.sh

ENTRYPOINT /bin/bash /run.sh