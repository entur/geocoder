FROM bellsoft/liberica-runtime-container:jre-25-slim-musl

WORKDIR /srv

COPY ./proxy-start.sh ./
COPY ./build/libs/proxy-all.jar proxy.jar
RUN chown -R 1000:1000 /srv
USER 1000:1000

CMD ["sh", "-c", "./proxy-start.sh"]
