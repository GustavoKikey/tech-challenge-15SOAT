# =====================================================================
# Providers — kubernetes e helm autenticam direto com os certificados
# expostos pelo recurso kind_cluster, sem depender do ~/.kube/config.
# Isso garante que o Terraform sempre fala com O CLUSTER QUE ELE CRIOU,
# mesmo que o kubectl da máquina esteja apontando para outro contexto.
# =====================================================================

provider "kubernetes" {
  host                   = kind_cluster.oficina.endpoint
  client_certificate     = kind_cluster.oficina.client_certificate
  client_key             = kind_cluster.oficina.client_key
  cluster_ca_certificate = kind_cluster.oficina.cluster_ca_certificate
}

provider "helm" {
  kubernetes {
    host                   = kind_cluster.oficina.endpoint
    client_certificate     = kind_cluster.oficina.client_certificate
    client_key             = kind_cluster.oficina.client_key
    cluster_ca_certificate = kind_cluster.oficina.cluster_ca_certificate
  }
}
