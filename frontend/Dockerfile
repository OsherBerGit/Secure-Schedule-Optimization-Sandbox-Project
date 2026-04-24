# ==========================================
# Stage 1: Build the React application
# ==========================================
FROM node:20-alpine AS builder
WORKDIR /app

# Copy package files and install dependencies
COPY package.json package-lock.json* ./
RUN npm install

# Copy source code and build the app
COPY . .
# If you have environment variables (like VITE_API_URL), they must be passed during this build step.
RUN npm run build

# ==========================================
# Stage 2: Serve with Nginx (Secure & Lightweight)
# ==========================================
FROM nginx:alpine

# Remove default Nginx website
RUN rm -rf /usr/share/nginx/html/*

# Copy the built static files from the builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy a custom Nginx configuration to support React Router (SPA)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]