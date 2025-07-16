FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build 

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/.next ./.next
RUN if [ -d /app/public ]; then \
      mkdir -p ./public && cp -r /app/public/* ./public/ ; \
    fi
COPY --from=build /app/package*.json ./
RUN npm i --omit=dev
EXPOSE 3000
CMD ["npm","start"]