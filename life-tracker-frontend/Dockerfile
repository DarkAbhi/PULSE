# --- deps ---
FROM node:20-alpine AS deps
WORKDIR /app
COPY life-tracker-frontend/package.json life-tracker-frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

# --- build ---
FROM node:20-alpine AS build
WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1
# Next.js resolves rewrites while building.
ARG INTERNAL_API_BASE_URL=http://backend:8080
ENV INTERNAL_API_BASE_URL=$INTERNAL_API_BASE_URL
COPY --from=deps /app/node_modules ./node_modules
COPY life-tracker-frontend/ .
COPY release.json /release.json
RUN --mount=type=cache,target=/app/.next/cache npm run build

# --- run ---
FROM node:20-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
ENV HOSTNAME=0.0.0.0
ENV PORT=3000
EXPOSE 3000

# app artifacts
COPY release.json /release.json
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public
# start Next
CMD ["node", "server.js"]
