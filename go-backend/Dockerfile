# build stage
FROM golang:1.25-alpine AS builder
WORKDIR /app
RUN apk add --no-cache git
COPY go-backend/go.mod go-backend/go.sum ./
RUN go mod download
COPY go-backend/ .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o /out/life-api ./cmd/api

# runtime stage
FROM alpine:3.20
WORKDIR /app
RUN apk add --no-cache curl tzdata
COPY --from=builder /out/life-api /app/life-api
COPY --from=builder /app/migrations /app/migrations
COPY release.json /release.json
EXPOSE 8080
# run as non-root (65532 is a common nobody UID)
USER 65532:65532
ENTRYPOINT ["/app/life-api"]
