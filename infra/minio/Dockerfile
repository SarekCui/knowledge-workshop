FROM golang:1.24.8-alpine AS builder

RUN apk add --no-cache git
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    GOPROXY=https://proxy.golang.org,direct \
    GOBIN=/out go install github.com/minio/minio@RELEASE.2025-10-15T17-29-55Z

FROM alpine:3.22

RUN apk add --no-cache ca-certificates curl && addgroup -S minio && adduser -S -G minio minio
COPY --from=builder /out/minio /usr/local/bin/minio
RUN mkdir -p /data && chown minio:minio /data
USER minio
EXPOSE 9000 9001
ENTRYPOINT ["minio"]
CMD ["server", "/data", "--console-address", ":9001"]
