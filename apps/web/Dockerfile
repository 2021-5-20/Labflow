FROM node:22-alpine AS build
WORKDIR /workspace
COPY package*.json ./
RUN npm install
COPY . .
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM node:22-alpine
WORKDIR /app
RUN npm install -g serve
COPY --from=build /workspace/dist ./dist
EXPOSE 5173
CMD ["serve", "-s", "dist", "-l", "5173"]
