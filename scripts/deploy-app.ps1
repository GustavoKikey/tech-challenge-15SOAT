<#
.SYNOPSIS
  Deploy da aplicacao no cluster kind (Windows).

.DESCRIPTION
  Pre-requisito: cluster provisionado com Terraform (pasta infra/).
  Passos executados:
    1. docker build da imagem da aplicacao
    2. kind load - copia a imagem para dentro dos nos do cluster
       (cluster local nao tem registry; sem isso o pod nao acha a imagem)
    3. kubectl apply dos manifestos k8s/
    4. aguarda o rollout do Deployment concluir

  OBS: arquivo mantido em ASCII puro (sem acentos) de proposito -
  o Windows PowerShell 5.1 le .ps1 sem BOM como ANSI e caracteres
  especiais quebrariam o parser em outras maquinas.

.EXAMPLE
  .\scripts\deploy-app.ps1
#>
param(
    [string]$Cluster   = "oficina",
    [string]$Namespace = "oficina",
    [string]$Image     = "oficina-mvp:latest"
)

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot   # scripts/ -> raiz do repositorio

Write-Host "==> [1/4] Build da imagem $Image" -ForegroundColor Cyan
docker build -t $Image $raiz
if ($LASTEXITCODE -ne 0) { throw "docker build falhou" }

Write-Host "==> [2/4] Carregando a imagem no cluster kind '$Cluster'" -ForegroundColor Cyan
kind load docker-image $Image --name $Cluster
if ($LASTEXITCODE -ne 0) { throw "kind load falhou - o cluster existe? (cd infra; terraform apply)" }

Write-Host "==> [3/4] Aplicando manifestos k8s/" -ForegroundColor Cyan
kubectl apply -f (Join-Path $raiz "k8s")
if ($LASTEXITCODE -ne 0) { throw "kubectl apply falhou" }

Write-Host "==> [4/4] Aguardando rollout do Deployment" -ForegroundColor Cyan
kubectl -n $Namespace rollout status deployment/oficina-app --timeout=300s
if ($LASTEXITCODE -ne 0) { throw "rollout nao concluiu - diagnostico: kubectl -n $Namespace get pods" }

Write-Host ""
Write-Host "Aplicacao no ar:" -ForegroundColor Green
Write-Host "  Painel de demonstracao: http://localhost:8080/"
Write-Host "  Swagger UI ...........: http://localhost:8080/swagger"
Write-Host "  Health ...............: http://localhost:8080/health"
