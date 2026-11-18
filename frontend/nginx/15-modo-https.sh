#!/bin/sh
# Roda na subida do container, antes do envsubst da imagem oficial (20-envsubst-on-templates.sh).
# Escolhe o modelo do site conforme HTTPS:
#   sim (padrão) o Nginx tem o certificado (/etc/nginx/certs/fullchain.pem e privkey.pem)
#   nao          atrás de um proxy que cuida do HTTPS; aceita o IP real vindo de PROXY_CONFIAVEL
set -eu

modelos=/etc/nginx/modelos
destino=/etc/nginx/templates/default.conf.template
mkdir -p /etc/nginx/templates

case "${HTTPS:-sim}" in
  sim|SIM|true|1)
    if [ ! -s /etc/nginx/certs/fullchain.pem ] || [ ! -s /etc/nginx/certs/privkey.pem ]; then
      echo "biblioteca: HTTPS=sim, mas faltam fullchain.pem e privkey.pem em /etc/nginx/certs." >&2
      echo "biblioteca: monte a pasta do certificado (CERTS_DIR) ou use HTTPS=nao atrás de um proxy." >&2
      exit 1
    fi
    dominio="${DOMINIO:-_}"
    case "$dominio" in
      *[!A-Za-z0-9._-]*) echo "biblioteca: DOMINIO inválido: '$dominio'." >&2; exit 1 ;;
    esac
    sed "s/\${DOMINIO}/$dominio/g" "$modelos/https.conf.template" > "$destino"
    echo "biblioteca: HTTPS no próprio Nginx (domínio $dominio)"
    ;;
  nao|não|NAO|false|0)
    : > /etc/nginx/snippets/proxy-confiavel.conf
    for faixa in ${PROXY_CONFIAVEL:-10.0.0.0/8 172.16.0.0/12 192.168.0.0/16}; do
      case "$faixa" in
        *[!0-9A-Fa-f.:/]*) echo "biblioteca: faixa inválida em PROXY_CONFIAVEL: '$faixa'." >&2; exit 1 ;;
      esac
      echo "set_real_ip_from $faixa;" >> /etc/nginx/snippets/proxy-confiavel.conf
    done
    cp "$modelos/http.conf.template" "$destino"
    echo "biblioteca: só HTTP, atrás de proxy (confia em: ${PROXY_CONFIAVEL:-faixas privadas})"
    ;;
  *)
    echo "biblioteca: HTTPS deve ser 'sim' ou 'nao' (veio '${HTTPS}')." >&2
    exit 1
    ;;
esac
