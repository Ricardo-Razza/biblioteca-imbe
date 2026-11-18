# Site público e gestão (Angular) servidos pelo Nginx, que também encaminha /api para o backend.
# HTTPS=sim (padrão) usa o certificado montado em /etc/nginx/certs; HTTPS=nao fica atrás de um proxy.
FROM node:24-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npx ng build

FROM nginx:1.28-alpine
COPY nginx/modelos /etc/nginx/modelos
COPY nginx/snippets /etc/nginx/snippets
COPY nginx/15-modo-https.sh /docker-entrypoint.d/15-modo-https.sh
RUN sed -i 's/\r$//' /docker-entrypoint.d/15-modo-https.sh && chmod +x /docker-entrypoint.d/15-modo-https.sh \
    && rm -f /etc/nginx/conf.d/default.conf && mkdir -p /etc/nginx/templates
COPY --from=build /src/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80 443
