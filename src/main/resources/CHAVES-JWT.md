# Chaves JWT deste diretório

`privateKey.pem` e `publicKey.pem` são um par RSA gerado **exclusivamente para
execução local e para a suíte de testes**.

**Não são as chaves de produção.** Em Kubernetes, o par real é montado por Secret em
`/etc/oficina/jwt` (ver `k8s/21-secret-jwt.yaml` e as variáveis
`MP_JWT_VERIFY_PUBLICKEY_LOCATION` / `SMALLRYE_JWT_SIGN_KEY_LOCATION` no ConfigMap), e a
Function Serverless recebe a chave privada do Secrets Manager, injetada pelo Terraform
no momento do apply.

Rotacionado em 2026-09-10.

> Os arquivos `.pem` não aceitam comentário: o SmallRye JWT falha ao assinar
> (`JwtSignatureException: SRJWT05009`) se houver qualquer texto antes de
> `-----BEGIN`. Por isso esta explicação vive aqui, e não dentro deles.
