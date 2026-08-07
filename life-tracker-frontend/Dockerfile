# --- deps ---
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json* pnpm-lock.yaml* yarn.lock* ./
RUN \
    if [ -f pnpm-lock.yaml ]; then npm i -g pnpm && pnpm i --frozen-lockfile; \
    elif [ -f yarn.lock ]; then yarn install --frozen-lockfile; \
    else npm ci; fi

# --- build ---
FROM node:20-alpine AS build
WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1
# Next.js inlines NEXT_PUBLIC_ variables into client bundles during `npm run build`.
ARG NEXT_PUBLIC_GEMINI_API_KEY
ENV NEXT_PUBLIC_GEMINI_API_KEY=$NEXT_PUBLIC_GEMINI_API_KEY
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# --- run (no-standalone) ---
FROM node:20-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
EXPOSE 3000

# install prod-only deps
COPY package.json package-lock.json* pnpm-lock.yaml* yarn.lock* ./
RUN \
    if [ -f pnpm-lock.yaml ]; then npm i -g pnpm && pnpm i --prod --frozen-lockfile; \
    elif [ -f yarn.lock ]; then yarn install --production --frozen-lockfile; \
    else npm ci --omit=dev; fi

# app artifacts
COPY --from=build /app/.next ./.next
COPY --from=build /app/public ./public
# start Next
CMD ["node", "node_modules/next/dist/bin/next", "start", "-p", "3000"]
