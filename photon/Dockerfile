FROM bellsoft/liberica-runtime-container:jre-25-musl

WORKDIR /srv

SHELL ["/bin/ash", "-eo", "pipefail", "-c"]

# photon-start.sh fetches photon_data.tar.gz over HTTPS on first start;
# the slim variant of this image has no TLS support.
# hadolint ignore=DL3018
RUN apk add --no-cache curl ca-certificates

COPY --chown=1000:1000 photon.jar photon-start.sh ./

USER 1000:1000

CMD ["./photon-start.sh"]
