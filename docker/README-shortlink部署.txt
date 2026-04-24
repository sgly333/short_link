在服务器（或本机）建一个目录，例如 shortlink，进入该目录后所有操作都在这里完成。



目录中建议包含：



  link.sql                 ← 从项目 resources/database/ 复制

  link-data.sql            ← 同上

  broker.conf              ← 从本仓库 docker/rocketmq/broker.conf 复制到当前目录并改名为 broker.conf



  docker-compose.infra.yml

  docker-compose.runtime.yml

  docker-compose.frontend.yml   ← 可选：部署控制台前端

  runtime.env              ← 由 runtime.env.example 复制后修改



  nginx/default.conf       ← 从本仓库 docker/nginx/default.conf 复制，保持 nginx/ 子目录

  dist/                    ← 前端构建产物（见下文「控制台前端」）



  shortlink-gateway-exec.jar

  shortlink-admin-1.0-SNAPSHOT-exec.jar   （若打包名不同请改 runtime 里 volumes）

  shortlink-project-exec.jar



命令（当前目录 = shortlink）：



  docker compose -f docker-compose.infra.yml up -d

  docker compose -f docker-compose.runtime.yml --env-file runtime.env up -d

  docker compose -f docker-compose.frontend.yml up -d



说明：compose 文件在仓库的 docker/ 子目录，上传到服务器时请把 yml、nginx、runtime.env.example 一并放到 shortlink 目录，或把整个 docker 目录内容拷到 shortlink 后再把 sql、jar、broker.conf 按上表补齐。



--- 控制台前端（console-vue）---



1. 在开发机进入 console-vue：

     npm install

     npm run build

2. 将生成的 dist 目录整体上传到 shortlink/dist/（与 docker-compose.frontend.yml 同级）。

3. 确保 default.conf 里网关地址与部署一致：

   - 使用本仓库 docker-compose.frontend.yml 时，默认通过 Docker 网络访问 shortlink-gateway:8000；

   - 若 Nginx 装在宿主机、网关映射在宿主机 8000，请把 nginx/default.conf 里 upstream 改为：

       server 127.0.0.1:8000;

4. 云安全组放行 80（或 NGINX_HTTP_PORT 映射的端口）。



前端 axios 使用相对路径 /api/short-link/admin/v1，由 Nginx 把 /api 转发到网关，与 vite 开发时代理一致。


