ARG FRONT_NODE_IMAGE=node:18-alpine
ARG FRONT_NGINX_IMAGE=nginx:1.27-alpine
FROM ${FRONT_NODE_IMAGE} AS build

WORKDIR /app
COPY exphlp-front/package*.json ./

ARG FRONT_HTTP_PROXY=
ARG FRONT_HTTPS_PROXY=
ARG NPM_REGISTRY=https://registry.npmmirror.com

# Handle proxy differences across developer machines:
# - By default, clear inherited proxy settings to avoid 127.0.0.1:* failures in containers.
# - If proxy is required, pass FRONT_HTTP_PROXY / FRONT_HTTPS_PROXY via compose args.
RUN set -eux; \
  unset HTTP_PROXY HTTPS_PROXY ALL_PROXY http_proxy https_proxy all_proxy NPM_CONFIG_PROXY NPM_CONFIG_HTTPS_PROXY npm_config_proxy npm_config_https_proxy; \
  npm config delete proxy || true; \
  npm config delete https-proxy || true; \
  npm config delete noproxy || true; \
  if [ -n "${FRONT_HTTP_PROXY}" ]; then npm config set proxy "${FRONT_HTTP_PROXY}"; fi; \
  if [ -n "${FRONT_HTTPS_PROXY}" ]; then npm config set https-proxy "${FRONT_HTTPS_PROXY}"; fi; \
  npm config set registry "${NPM_REGISTRY}"; \
  npm ci --no-audit --no-fund
COPY exphlp-front/ ./

ARG VUE_APP_BASE_API=
ENV VUE_APP_BASE_API=${VUE_APP_BASE_API}
RUN npm run build:prod

FROM ${FRONT_NGINX_IMAGE}
COPY docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
