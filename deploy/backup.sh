#!/usr/bin/env bash
# Backup do banco (mysqldump) e das capas, com os containers no ar. Roda no servidor, na raiz do repositório:
#   deploy/backup.sh                 # grava em ./backups
#   BACKUP_DIR=/mnt/backup deploy/backup.sh
# Agendar no cron do servidor, por exemplo todo dia às 2h:
#   0 2 * * * cd /opt/biblioteca-imbe && deploy/backup.sh >> /var/log/biblioteca-backup.log 2>&1
# Guarda os últimos BACKUP_DIAS dias (padrão 30). Copie a pasta para outro lugar (outro servidor, nuvem da
# Prefeitura): backup no mesmo disco não protege contra perda do disco.
set -euo pipefail

cd "$(dirname "$0")/.."
COMPOSE="${COMPOSE:-docker compose -f docker-compose.prod.yml}"
DESTINO="${BACKUP_DIR:-./backups}"
DIAS="${BACKUP_DIAS:-30}"
AGORA="$(date +%Y-%m-%d_%H%M)"
mkdir -p "$DESTINO"

echo "[$(date -Is)] Backup em $DESTINO"

# --single-transaction: cópia consistente sem travar as tabelas (InnoDB). A senha fica dentro do container.
$COMPOSE exec -T mysql sh -c \
  'exec mysqldump --single-transaction --routines --triggers --no-tablespaces -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
  | gzip > "$DESTINO/banco_$AGORA.sql.gz.parcial"
mv "$DESTINO/banco_$AGORA.sql.gz.parcial" "$DESTINO/banco_$AGORA.sql.gz"

$COMPOSE exec -T backend tar -C /dados -czf - capas > "$DESTINO/capas_$AGORA.tar.gz.parcial"
mv "$DESTINO/capas_$AGORA.tar.gz.parcial" "$DESTINO/capas_$AGORA.tar.gz"

find "$DESTINO" -maxdepth 1 \( -name 'banco_*.sql.gz' -o -name 'capas_*.tar.gz' \) -mtime +"$DIAS" -delete

echo "[$(date -Is)] Concluído: banco_$AGORA.sql.gz e capas_$AGORA.tar.gz"
