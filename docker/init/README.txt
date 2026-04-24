（可选）若你希望 SQL 放在子目录 init/，可把 compose 里 mysql 的 volumes 改回：
  ./init/01-link.sql
  ./init/02-link-data.sql

当前默认约定：link.sql、link-data.sql 与 docker-compose 同在 shortlink 根目录，见 README-shortlink部署.txt。
