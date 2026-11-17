#!/usr/bin/env bash
# Restaura um backup feito pelo deploy/backup.sh. APAGA o banco e as capas atuais.
#   deploy/restaurar.sh backups/banco_2026-11-17_0200.sql.gz backups/capas_2026-11-17_0200.tar.gz
set -euo pipefail

if [ $# -ne 2 ]; then
  echo "Uso: $0 <banco_*.sql.gz> <capas_*.tar.gz>" >&2
  exit 1
fi
BANCO="$(realpath "$1")"
CAPAS="$(realpath "$2")"
cd "$(dirname "$0")/.."
COMPOSE="${COMPOSE:-docker compose -f docker-compose.prod.yml}"

read -r -p "Isto substitui TODO o banco e as capas atuais. Digite RESTAURAR para continuar: " resposta
[ "$resposta" = "RESTAURAR" ] || { echo "Cancelado."; exit 1; }

$COMPOSE stop backend
$COMPOSE exec -T mysql sh -c \
  'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "DROP DATABASE IF EXISTS \`$MYSQL_DATABASE\`; CREATE DATABASE \`$MYSQL_DATABASE\`; GRANT ALL ON \`$MYSQL_DATABASE\`.* TO \`$MYSQL_USER\`@\`%\`;"'
gunzip -c "$BANCO" | $COMPOSE exec -T mysql sh -c 'exec mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'

$COMPOSE run --rm --no-deps --entrypoint sh -v "$CAPAS:/tmp/capas.tar.gz:ro" -u root backend \
  -c 'rm -rf /dados/capas/* && tar -C /dados -xzf /tmp/capas.tar.gz && chown -R biblioteca:biblioteca /dados/capas'

$COMPOSE start backend
echo "Restaurado. Confira a gestão e o site."
