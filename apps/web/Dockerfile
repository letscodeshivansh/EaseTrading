# ---- Build stage ----
FROM node:20-alpine AS build
WORKDIR /app

# Corporate networks often intercept HTTPS and present their own certificate, which
# npm doesn't recognise — causing UNABLE_TO_GET_ISSUER_CERT_LOCALLY when installing
# from the npm registry. Telling npm to tolerate it lets the local build proceed.
# This only affects dependency downloads during the image build, not the app.
RUN npm config set strict-ssl false

COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

# ---- Run stage: serve the optimized standalone build ----
FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
# Next.js "standalone" output bundles only what's needed to run.
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public
EXPOSE 3000
CMD ["node", "server.js"]
