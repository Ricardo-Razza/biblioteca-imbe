# Certificado HTTPS

O Nginx lê daqui (montado em `/etc/nginx/certs`, só leitura):

- `fullchain.pem`: certificado com a cadeia intermediária
- `privkey.pem`: chave privada

Os `.pem` ficam fora do git (`.gitignore`). Detalhes no README, seção "Implantação".
