# ADR 005 — Um state do Terraform por ambiente

- **Status:** aceito
- **Data:** 2026-09-10
- **Relacionado:** [ADR 004](adr-004-padrao-de-comunicacao.md)

---

## Contexto

Cada um dos três repositórios de infraestrutura guarda seu state no bucket S3 comum.
A primeira versão declarava a chave fixa no código:

```hcl
backend "s3" {
  key = "infra-k8s/terraform.tfstate"
}
```

O ambiente entrava apenas como **variável** (`-var="ambiente=prod"`), usada para nomear
recursos: `oficina-hom`, `oficina-prod`.

Isso parece suficiente e não é.

## O problema

O Terraform decide o que fazer comparando a configuração desejada com **o state**, não
com a nuvem. Com uma chave única por módulo, os dois ambientes disputam o mesmo state.

Aplicar `prod` sobre um state que descreve `hom` não é lido como "crie um segundo
ambiente". É lido como **"os recursos que eu gerencio mudaram de nome"** — e o plano
resultante destrói tudo que existe para recriar com o nome novo.

Foi exatamente o que aconteceu: um `apply` de `prod` destruiu o cluster `oficina-hom`,
com o banco e a Function ainda em processo de conversão quando a execução foi
interrompida.

O erro é silencioso porque **nada na configuração está errado**: `validate` passa,
`plan` passa, e o apply faz precisamente o que foi pedido.

## Decisão

**A chave do state carrega o ambiente**, e sai do código:

```
infra-k8s/hom/terraform.tfstate
infra-k8s/prod/terraform.tfstate
infra-database/hom/terraform.tfstate
...
```

O bloco `backend "s3"` declara só região e criptografia; bucket, tabela de lock e chave
chegam por `-backend-config` no `init`:

```bash
terraform init -reconfigure \
  -backend-config="bucket=$TF_STATE_BUCKET" \
  -backend-config="dynamodb_table=$TF_LOCK_TABLE" \
  -backend-config="key=infra-k8s/${AMBIENTE}/terraform.tfstate"
```

Nos workflows, o ambiente vem da branch — `main` → `prod`, demais → `hom`. No job de
`plan`, que roda em Pull Request, a origem é `github.base_ref`; nos jobs de `apply`,
`github.ref_name`.

## Alternativas consideradas

| Alternativa | Por que não |
| --- | --- |
| **Terraform workspaces** | Resolve o isolamento e é nativo. Perde em legibilidade: o state fica em `env:/prod/...`, e é fácil aplicar no workspace errado por esquecer um `workspace select`. A chave explícita deixa o ambiente visível no comando. |
| **Um bucket por ambiente** | Isolamento mais forte, ao custo de duplicar o bootstrap e a configuração de secrets. Desproporcional para dois ambientes. |
| **Diretórios separados por ambiente** (`envs/hom`, `envs/prod`) | Duplicaria o código de infraestrutura ou exigiria módulos compartilhados. Mais movimento do que o problema pede. |

## Consequências

**Positivas**

- Ambientes verdadeiramente isolados: aplicar um não enxerga nem toca o outro.
- O ambiente fica explícito no comando de `init`, difícil de errar em silêncio.
- Um lock por ambiente — `hom` e `prod` podem ser aplicados em paralelo.

**Negativas / custos assumidos**

- O `init` exige três parâmetros em vez de dois; quem rodar `terraform init` na mão sem
  eles cai num state local vazio e vê um plano que quer criar tudo de novo. Os scripts
  em `scripts/fase-3/` encapsulam isso.
- States criados antes desta mudança ficam órfãos na chave antiga
  (`infra-k8s/terraform.tfstate`) e precisam ser migrados ou descartados.

## Lição

Nomear recursos por ambiente **não** separa ambientes. Quem separa é o state — é ele
que define o que o Terraform considera "seu" e, portanto, o que ele se sente livre para
destruir.
