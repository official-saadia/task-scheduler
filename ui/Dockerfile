# ---------- build ----------
# Base image for the build stage: Node 22 (LTS) on Alpine, which compiles the
# Vite bundle. Pinned to a patch version so a rebuild months from now resolves
# to the same image rather than whatever 22-alpine points at then.
FROM node:22.11.0-alpine AS build
WORKDIR /build

COPY ui/package.json ui/package-lock.json ./
RUN npm ci

COPY ui/ ./

# Vite inlines VITE_* variables at BUILD time — they are baked into the bundle,
# not read at runtime. So this has to be a build arg; setting it as a container
# env var in compose would do nothing.
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# ---------- runtime ----------
# Base image for the runtime stage: Apache httpd 2.4, which serves the built
# static files and reverse-proxies the API. Pinned to a patch version for the
# same reproducibility reason as the build stage above.
FROM httpd:2.4.62

# mod_rewrite drives the SPA fallback; mod_proxy/mod_proxy_http do the API proxy.
# Unlike nginx these are separate modules and ship disabled, so enable them.
RUN sed -i \
    -e 's|^#\(LoadModule rewrite_module .*\)|\1|' \
    -e 's|^#\(LoadModule proxy_module .*\)|\1|' \
    -e 's|^#\(LoadModule proxy_http_module .*\)|\1|' \
    conf/httpd.conf \
 && echo "Include conf/extra/app.conf" >> conf/httpd.conf

COPY ui/apache.conf conf/extra/app.conf
COPY --from=build /build/dist /usr/local/apache2/htdocs/
EXPOSE 80
