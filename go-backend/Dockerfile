# build stage
FROM golang:1.24.3-alpine AS builder
WORKDIR /app
RUN apk add --no-cache git
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o /out/life-api ./cmd/api

# runtime stage
FROM alpine:3.20
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=builder /out/life-api /app/life-api
COPY --from=builder /app/migrations /app/migrations
EXPOSE 8080
# run as non-root (65532 is a common nobody UID)
USER 65532:65532
ENTRYPOINT ["/app/life-api"]