#!/usr/bin/env python3
"""
Proteção das branches main dos quatro repositórios.

O desafio pede a main protegida, sem commits diretos e com merge por Pull
Request. Marcar "Require a pull request" na interface não basta:

  - sem `enforce_admins`, a regra não vale para quem administra o
    repositório — que é exatamente quem empurra direto na pressa. O push
    passa e ninguém percebe;

  - sem `required_status_checks`, a CI roda, pode ficar vermelha, e o botão
    de merge continua verde. A pipeline vira decoração.

Os nomes dos checks NÃO são escritos à mão
------------------------------------------
Eles são lidos da última execução disparada por pull_request em cada
repositório. Duas razões:

1. nome exigido tem que bater byte a byte com o nome que a CI publica. Uma
   versão anterior deste script montava a lista no shell e passava por
   stdin; no Windows o Python decodificou com a codepage do console e
   gravou "unitÃ¡rios" onde a CI publica "unitários". O GitHub passou a
   esperar um check que nunca chegaria, e NENHUM Pull Request, em nenhum
   dos quatro repositórios, podia mais ser mergeado;

2. job renomeado no workflow deixa de existir como check. Perguntando ao
   repositório, o script se corrige sozinho na próxima execução.

Só entram jobs de execuções disparadas por `pull_request`: um job que só
roda em push na main — 'Apply (prod)', por exemplo — nunca reportaria num
Pull Request, e exigi-lo travaria o merge para sempre.

Uso:
    python scripts/fase-3/proteger-branches.py
    python scripts/fase-3/proteger-branches.py --conferir
    python scripts/fase-3/proteger-branches.py --soltar    # remove os checks
"""
import json
import subprocess
import sys

# O console do Windows abre em cp1252, e imprimir acento aborta o script no
# meio — depois de já ter alterado parte dos repositórios. Errors='replace'
# porque saída ilegível é ruim, mas falhar na metade de uma alteração é pior.
for fluxo in (sys.stdout, sys.stderr):
    try:
        fluxo.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

USUARIO = "GustavoKikey"
REPOS = [
    "tech-challenge-15SOAT",
    "oficina-infra-k8s",
    "oficina-infra-database",
    "oficina-auth-lambda",
]


def gh(*args, entrada=None):
    """Chama o gh sempre decodificando como UTF-8, e não como a codepage
    do console — foi exatamente aí que a versão anterior se perdeu."""
    r = subprocess.run(
        ["gh", *args],
        input=entrada.encode("utf-8") if entrada else None,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if r.returncode != 0:
        raise RuntimeError(r.stderr.decode("utf-8", "replace")[:400])
    return r.stdout.decode("utf-8")


def checks_do_repo(repo):
    """Nomes dos jobs que a CI publica num Pull Request contra a main."""
    runs = json.loads(gh(
        "run", "list", "--repo", f"{USUARIO}/{repo}",
        "--limit", "25", "--json", "databaseId,event,conclusion",
    ))
    candidatos = [r for r in runs if r["event"] == "pull_request"]
    if not candidatos:
        return []

    nomes = []
    for run in candidatos[:3]:
        jobs = json.loads(gh(
            "run", "view", str(run["databaseId"]), "--repo", f"{USUARIO}/{repo}",
            "--json", "jobs",
        ))["jobs"]
        for j in jobs:
            # 'skipped' é o job de apply, que só roda em push. Exigi-lo
            # travaria o merge à espera de algo que nunca vem.
            if j["conclusion"] == "success" and j["name"] not in nomes:
                nomes.append(j["name"])
    return nomes


def aplicar(repo, contextos):
    corpo = {
        "required_status_checks": {"strict": False, "contexts": contextos},
        "enforce_admins": True,
        "required_pull_request_reviews": {
            # Zero de propósito: num trabalho individual, exigir aprovação
            # de terceiro trava tudo — o GitHub não deixa ninguém aprovar
            # o próprio Pull Request.
            "dismiss_stale_reviews": False,
            "require_code_owner_reviews": False,
            "required_approving_review_count": 0,
        },
        "restrictions": None,
    }
    gh("api", "-X", "PUT",
       f"repos/{USUARIO}/{repo}/branches/main/protection",
       "--input", "-", entrada=json.dumps(corpo))


def estado(repo):
    try:
        d = json.loads(gh("api", f"repos/{USUARIO}/{repo}/branches/main/protection"))
    except RuntimeError:
        return "sem proteção"
    ctx = (d.get("required_status_checks") or {}).get("contexts", [])
    return (f"admins={d['enforce_admins']['enabled']}  "
            f"PR={d.get('required_pull_request_reviews') is not None}  "
            f"checks={ctx}")


if __name__ == "__main__":
    modo = sys.argv[1] if len(sys.argv) > 1 else ""

    for repo in REPOS:
        print(f"\n══ {USUARIO}/{repo}")

        if modo == "--conferir":
            print("   " + estado(repo))
            continue

        if modo == "--soltar":
            aplicar(repo, [])
            print("   checks removidos — " + estado(repo))
            continue

        contextos = checks_do_repo(repo)
        if not contextos:
            print("   nenhuma execução de Pull Request encontrada; "
                  "abra um Pull Request primeiro e rode de novo")
            continue
        aplicar(repo, contextos)
        print("   " + estado(repo))

    if modo not in ("--conferir", "--soltar"):
        print("\nCommit direto na main passa a ser recusado — inclusive o seu.")
        print("Todo merge exige Pull Request com esses checks verdes.")
