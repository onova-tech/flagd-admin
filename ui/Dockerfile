FROM docker.io/node:20-alpine AS builder

WORKDIR /build

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM docker.io/nginx:alpine

RUN apk add --no-cache wget

ARG VITE_API_BASE_URL=http://localhost:9090
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

COPY --from=builder /build/dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:80 || exit 1

CMD ["nginx", "-g", "daemon off;"]
