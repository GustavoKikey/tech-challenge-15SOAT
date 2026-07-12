# =====================================================================
# metrics-server — dependência do Horizontal Pod Autoscaler
# ---------------------------------------------------------------------
# O HPA (k8s/50-hpa.yaml) decide a escala consultando métricas de
# CPU/memória dos pods via Metrics API — e quem materializa essa API é
# o metrics-server, que NÃO vem instalado por padrão no kind.
#
# Instalado via chart Helm oficial. A flag --kubelet-insecure-tls é
# necessária em clusters kind: os certificados dos kubelets locais não
# incluem os IPs dos nós (containers) como SAN.
# =====================================================================

resource "helm_release" "metrics_server" {
  name       = "metrics-server"
  repository = "https://kubernetes-sigs.github.io/metrics-server/"
  chart      = "metrics-server"
  version    = var.metrics_server_chart_version
  namespace  = "kube-system"

  set {
    name  = "args[0]"
    value = "--kubelet-insecure-tls"
  }

  depends_on = [kind_cluster.oficina]
}
