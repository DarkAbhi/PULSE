# --- deps ---
FROM node:20-alpine AS deps
WORKDIR /app
COPY life-tracker-frontend/package.json life-tracker-frontend/package-lock.json ./
RUN \
    if [ -f pnpm-lock.yaml ]; then npm i -g pnpm && pnpm i --frozen-lockfile; \
    elif [ -f yarn.lock ]; then yarn install --frozen-lockfile; \
    else npm ci; fi

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
RUN npm run build

# --- run (no-standalone) ---
FROM node:20-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
EXPOSE 3000

# install prod-only deps
COPY life-tracker-frontend/package.json life-tracker-frontend/package-lock.json ./
RUN \
    if [ -f pnpm-lock.yaml ]; then npm i -g pnpm && pnpm i --prod --frozen-lockfile; \
    elif [ -f yarn.lock ]; then yarn install --production --frozen-lockfile; \
    else npm ci --omit=dev; fi

# app artifacts
COPY release.json /release.json
COPY --from=build /app/.next ./.next
COPY --from=build /app/public ./public
# start Next
CMD ["node", "node_modules/next/dist/bin/next", "start", "-p", "3000"]
