FROM docker.1ms.run/library/node:20-alpine AS builder

WORKDIR /app

# 复制 package 文件
COPY package*.json ./

# 安装依赖
RUN npm install

# 复制源代码
COPY . .

# 构建生产版本
RUN npm run build -- --configuration production

# 第二阶段：Nginx 镜像
FROM docker.1ms.run/library/nginx:alpine

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/nginx.conf

# 从构建阶段复制静态资源
COPY --from=builder /app/dist/algorithm-viz-frontend /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
