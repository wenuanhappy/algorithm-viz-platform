FROM docker.1ms.run/library/nginx:alpine

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/nginx.conf

# 复制静态资源
COPY dist/ /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
