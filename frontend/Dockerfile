# Site público e gestão (Angular) servidos pelo Nginx, que também encaminha /api para o backend.
FROM node:24-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npx ng build

FROM nginx:1.28-alpine
COPY nginx/templates /etc/nginx/templates
COPY nginx/snippets /etc/nginx/snippets
COPY --from=build /src/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80 443
