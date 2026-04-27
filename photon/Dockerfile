FROM bellsoft/liberica-runtime-container:jre-25-musl

SHELL ["/bin/ash", "-eo", "pipefail", "-c"]

# photon-start.sh fetches photon_data.tar.gz over HTTPS on first start;
# the slim variant of this image has no TLS support. /srv must be owned
# by 1000 so the unprivileged process can write photon_data/ at runtime.
# hadolint ignore=DL3018
RUN apk add --no-cache curl ca-certificates && \
    install -d -o 1000 -g 1000 /srv

WORKDIR /srv

COPY --chown=1000:1000 photon.jar photon-start.sh ./

USER 1000:1000

CMD ["./photon-start.sh"]
