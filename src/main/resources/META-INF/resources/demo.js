/*
 * Painel de demonstração das APIs — Tech Challenge Fase 2.
 *
 * Sem framework: os passos são declarados no array PASSOS e renderizados como
 * cards. Cada execução mostra método + endpoint + payload resolvido + resposta,
 * e captura ids (token, serviçoId, peçaId, osId) para os passos seguintes via
 * placeholders {{...}} nos paths e payloads.
 *
 * Além do roteiro guiado há o MODO LIVRE (monte qualquer requisição, com
 * presets de todos os endpoints) e controles de sessão/contexto no cabeçalho.
 */

const estado = { accessToken: null, servicoId: null, pecaId: null, osId: null };

const PASSOS = [
    {
        grupo: "1 · Autenticação",
        titulo: "Login (JWT)",
        descricao: "Autentica com o admin provisionado no startup. O token retornado é usado automaticamente nos passos protegidos. Use o botão “Sair” no topo para descartá-lo.",
        metodo: "POST",
        path: "/auth/login",
        payload: {
            username: "admin",
            password: "admin123"
        },
        captura: (body) => { estado.accessToken = body.accessToken; }
    },
    {
        grupo: "2 · Catálogo e estoque (pré-requisitos)",
        titulo: "Cadastrar serviço no catálogo",
        descricao: "Cria o serviço de mão-de-obra que será usado na abertura da OS.",
        metodo: "POST",
        path: "/servicos",
        payload: {
            descricao: "Troca de pastilha de freio",
            valorBase: "150.00"
        },
        captura: (body) => { estado.servicoId = body.id; }
    },
    {
        grupo: "2 · Catálogo e estoque (pré-requisitos)",
        titulo: "Cadastrar peça no estoque",
        descricao: "Cria a peça com saldo inicial 4. Repare no campo saldoDisponivel — ele muda quando o orçamento reserva/baixa/devolve.",
        metodo: "POST",
        path: "/pecas",
        payload: {
            descricao: "Pastilha de freio dianteira",
            valorUnitario: "80.00",
            quantidadeInicial: 4
        },
        captura: (body) => { estado.pecaId = body.id; }
    },
    {
        grupo: "3 · Fase 2 — Abertura e status da OS",
        titulo: "Abertura de OS (dados completos)",
        descricao: "Requisito fase 2: recebe cliente, veículo, serviços e peças numa única chamada e retorna a identificação única da OS. Cliente/veículo que não existem são cadastrados na hora (get-or-create).",
        metodo: "POST",
        path: "/ordens-servico",
        payload: {
            cliente: {
                documento: "90000000922",
                nome: "Cliente Demonstração",
                email: "cliente.demo@email.com",
                telefone: "11999990000"
            },
            veiculo: { placa: "DEM0A12", marca: "GM", modelo: "Onix", ano: 2021 },
            servicos: [{ servicoId: "{{servicoId}}", valorCobrado: null }],
            pecas: [{ pecaId: "{{pecaId}}", quantidade: 2 }]
        },
        captura: (body) => { estado.osId = body.id; }
    },
    {
        grupo: "3 · Fase 2 — Abertura e status da OS",
        titulo: "Consulta de status (endpoint público)",
        descricao: "Requisito fase 2: situação atual da OS com a descrição amigável (Recebida, Diagnóstico, Aguardando Aprovação, Execução, Finalizada, Entregue). Sem autenticação — é o link do cliente.",
        metodo: "GET",
        path: "/publico/ordens-servico/{{osId}}/status"
    },
    {
        grupo: "4 · Fluxo da OS até o orçamento",
        titulo: "Iniciar diagnóstico",
        descricao: "RECEBIDA → EM_DIAGNOSTICO. O cliente é notificado por e-mail a cada mudança de status (mock logado no console do Quarkus em dev).",
        metodo: "POST",
        path: "/ordens-servico/{{osId}}/diagnostico"
    },
    {
        grupo: "4 · Fluxo da OS até o orçamento",
        titulo: "Gerar orçamento (reserva peças)",
        descricao: "Soma serviços + peças e RESERVA as peças no estoque, tudo numa transação. Consulte a peça no passo seguinte para ver o saldo disponível cair.",
        metodo: "POST",
        path: "/ordens-servico/{{osId}}/orcamento"
    },
    {
        grupo: "4 · Fluxo da OS até o orçamento",
        titulo: "Consultar peça (efeito no estoque)",
        descricao: "Evidência da reserva: quantidadeTotal continua 4, saldoDisponivel caiu para 2. Após recusa volta a 4; após aprovação a baixa reduz o total.",
        metodo: "GET",
        path: "/pecas/{{pecaId}}"
    },
    {
        grupo: "4 · Fluxo da OS até o orçamento",
        titulo: "Enviar orçamento para o cliente",
        descricao: "EM_DIAGNOSTICO → AGUARDANDO_APROVACAO. A partir daqui a decisão é do cliente.",
        metodo: "POST",
        path: "/ordens-servico/{{osId}}/orcamento/enviar"
    },
    {
        grupo: "5 · Fase 2 — Decisão do cliente (webhook público)",
        titulo: "Aprovar ou recusar o orçamento",
        descricao: "Requisito fase 2: endpoint público que recebe a notificação externa da decisão. aprovado=true → EM_EXECUCAO (baixa as reservas); aprovado=false → CANCELADA (devolve as peças ao estoque e a OS sai da listagem — exclusão lógica). Edite o payload para testar a recusa.",
        metodo: "POST",
        path: "/publico/ordens-servico/{{osId}}/orcamento/decisao",
        payload: { aprovado: true }
    },
    {
        grupo: "6 · Encerramento",
        titulo: "Finalizar serviço",
        descricao: "EM_EXECUCAO → FINALIZADA (mecânico terminou). Só funciona se o orçamento foi APROVADO.",
        metodo: "POST",
        path: "/ordens-servico/{{osId}}/finalizar"
    },
    {
        grupo: "6 · Encerramento",
        titulo: "Entregar veículo",
        descricao: "FINALIZADA → ENTREGUE. A OS sai da listagem padrão (exclusão lógica), mas continua no banco.",
        metodo: "POST",
        path: "/ordens-servico/{{osId}}/entregar"
    },
    {
        grupo: "7 · Fase 2 — Listagem ordenada",
        titulo: "Listagem operacional",
        descricao: "Requisito fase 2: ordenação Em Execução > Aguardando Aprovação > Diagnóstico > Recebida, mais antigas primeiro, SEM finalizadas/entregues/canceladas.",
        metodo: "GET",
        path: "/ordens-servico"
    },
    {
        grupo: "7 · Fase 2 — Listagem ordenada",
        titulo: "Exclusão lógica (filtro explícito)",
        descricao: "As OS encerradas não sumiram do banco: com o filtro explícito de status elas aparecem. ENTREGUE casa com o fim do fluxo guiado; para ver uma recusa, use o modo livre com ?status=CANCELADA.",
        metodo: "GET",
        path: "/ordens-servico?status=ENTREGUE"
    }
];

/* Presets do MODO LIVRE — cobre os demais endpoints da API. */
const PRESETS = [
    { rotulo: "— escolha um preset —" },
    { rotulo: "Health check", metodo: "GET", path: "/health" },
    { rotulo: "Listar clientes", metodo: "GET", path: "/clientes" },
    { rotulo: "Cadastrar cliente", metodo: "POST", path: "/clientes",
      payload: { nome: "Maria Souza", documento: "50000000566", email: "maria@email.com", telefone: "11988887777" } },
    { rotulo: "Listar veículos", metodo: "GET", path: "/veiculos" },
    { rotulo: "Listar serviços", metodo: "GET", path: "/servicos" },
    { rotulo: "Listar peças", metodo: "GET", path: "/pecas" },
    { rotulo: "Adicionar saldo à peça", metodo: "POST", path: "/pecas/{{pecaId}}/saldo",
      payload: { quantidade: 10 } },
    { rotulo: "Detalhe completo da OS", metodo: "GET", path: "/ordens-servico/{{osId}}" },
    { rotulo: "Status da OS (autenticado)", metodo: "GET", path: "/ordens-servico/{{osId}}/status" },
    { rotulo: "Resumo público da OS", metodo: "GET", path: "/publico/ordens-servico/{{osId}}" },
    { rotulo: "Recusar orçamento (rota interna)", metodo: "POST", path: "/ordens-servico/{{osId}}/orcamento/recusar" },
    { rotulo: "Aprovar orçamento (rota interna)", metodo: "POST", path: "/ordens-servico/{{osId}}/orcamento/aprovar" },
    { rotulo: "Cadastrar usuário (ADMIN)", metodo: "POST", path: "/auth/usuarios",
      payload: { username: "atendente.demo", password: "SenhaForte123!", role: "ATENDENTE" } },
    { rotulo: "Relatório: tempo médio de execução", metodo: "GET", path: "/relatorios/tempo-medio-execucao" }
];

/* ---------------- infraestrutura do painel ---------------- */

const $ = (sel) => document.querySelector(sel);

function resolver(texto) {
    return texto.replace(/\{\{(\w+)\}\}/g, (m, chave) =>
        estado[chave] != null ? estado[chave] : m);
}

function pendencias(texto) {
    return [...texto.matchAll(/\{\{(\w+)\}\}/g)].map(m => m[1]);
}

function atualizarContexto() {
    const auth = $("#chip-auth");
    auth.textContent = estado.accessToken ? "🔓 autenticado (clique p/ copiar token)" : "🔒 não autenticado";
    auth.classList.toggle("ativo", !!estado.accessToken);
    marcarChip("#chip-servico", "serviçoId", estado.servicoId);
    marcarChip("#chip-peca", "peçaId", estado.pecaId);
    marcarChip("#chip-os", "OS", estado.osId);
}

function marcarChip(sel, rotulo, valor) {
    const el = $(sel);
    el.textContent = `${rotulo}: ${valor ? valor.substring(0, 8) + "…" : "—"} ✏️`;
    el.classList.toggle("ativo", !!valor);
    el.title = valor ? valor + " (clique para editar)" : "clique para colar um id manualmente";
}

/* ---- controles do cabeçalho: logout / contexto / histórico ---- */

function sair() {
    estado.accessToken = null;
    atualizarContexto();
    mostrarResposta(200, "Sessão local encerrada — o token JWT foi descartado.\n" +
        "Os próximos passos protegidos responderão 401 até novo login.");
    $("#resp-status").textContent = "logout";
    $("#resp-status").className = "status-badge ok";
}

function limparContexto() {
    estado.servicoId = null;
    estado.pecaId = null;
    estado.osId = null;
    atualizarContexto();
}

function limparHistorico() {
    $("#historico").innerHTML = "";
}

function editarChip(chave, rotulo) {
    const valor = prompt(`Cole o ${rotulo} (vazio para limpar):`, estado[chave] || "");
    if (valor === null) return;               // cancelou
    estado[chave] = valor.trim() || null;
    atualizarContexto();
}

function copiarToken() {
    if (!estado.accessToken) { alert("Faça login primeiro (passo 1)."); return; }
    navigator.clipboard?.writeText(estado.accessToken);
    alert("Token copiado — cole no botão Authorize do Swagger, se quiser comparar.");
}

/* ---------------- execução de requisições ---------------- */

async function chamarApi(metodo, url, corpo) {
    mostrarRequisicao(metodo, url, corpo);
    const resp = await fetch(url, {
        method: metodo,
        headers: {
            ...(corpo ? { "Content-Type": "application/json" } : {}),
            ...(estado.accessToken ? { "Authorization": "Bearer " + estado.accessToken } : {})
        },
        body: corpo ? JSON.stringify(corpo) : undefined
    });
    const texto = await resp.text();
    mostrarResposta(resp.status, texto);
    registrarHistorico(metodo, url, resp.status, texto, corpo);
    return { status: resp.status, ok: resp.ok, texto };
}

/* Destaque de sintaxe simples para JSON já formatado (escapa HTML antes). */
function destacarJson(json) {
    const escapado = json
        .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
    return escapado.replace(
        /("(?:\\u[a-fA-F0-9]{4}|\\[^u]|[^\\"])*"(?:\s*:)?|\b(?:true|false|null)\b|-?\d+(?:\.\d+)?(?:[eE][+\-]?\d+)?)/g,
        (token) => {
            let classe = "j-num";
            if (token.startsWith('"')) classe = token.endsWith(":") ? "j-chave" : "j-texto";
            else if (token === "true" || token === "false") classe = "j-bool";
            else if (token === "null") classe = "j-nulo";
            return `<span class="${classe}">${token}</span>`;
        });
}

function mostrarRequisicao(metodo, url, corpo) {
    const linha = $("#req-linha");
    linha.classList.remove("vazio");
    linha.innerHTML = `<span class="metodo ${metodo}">${metodo}</span> ${url}`;
    const reqBody = $("#req-body");
    reqBody.classList.remove("vazio");
    reqBody.innerHTML = corpo
        ? destacarJson(JSON.stringify(corpo, null, 2))
        : "(sem payload)";
}

function mostrarResposta(status, corpoTexto) {
    const badge = $("#resp-status");
    badge.textContent = status;
    badge.className = "status-badge " + (status >= 200 && status < 300 ? "ok" : "erro");
    const el = $("#resp-body");
    el.classList.remove("vazio");
    try {
        el.innerHTML = destacarJson(JSON.stringify(JSON.parse(corpoTexto), null, 2));
    } catch {
        el.textContent = corpoTexto || "(sem corpo)";
    }
}

function registrarHistorico(metodo, url, status, corpoTexto, corpoReq) {
    const li = document.createElement("li");
    const ok = status >= 200 && status < 300;
    li.innerHTML = `<span class="h-status ${ok ? "ok" : "erro"}">${status}</span>
        <span class="metodo ${metodo}">${metodo}</span>
        <span class="h-caminho">${url}</span>`;
    li.onclick = () => { mostrarRequisicao(metodo, url, corpoReq); mostrarResposta(status, corpoTexto); };
    $("#historico").prepend(li);
}

function validarEExecutar(metodo, pathBruto, textoPayload, inline, botao, captura) {
    let corpo = null;
    if (textoPayload && textoPayload.trim()) {
        try {
            corpo = JSON.parse(resolver(textoPayload));
        } catch (e) {
            inline.textContent = "payload não é JSON válido";
            inline.className = "resultado-inline erro";
            return;
        }
    }

    const url = resolver(pathBruto);
    const faltando = pendencias(url).concat(corpo ? pendencias(JSON.stringify(corpo)) : []);
    if (faltando.length) {
        inline.textContent = `contexto faltando: ${faltando.join(", ")} (execute o passo que gera ou edite o chip no topo)`;
        inline.className = "resultado-inline erro";
        return;
    }

    botao.disabled = true;
    inline.textContent = "…";
    inline.className = "resultado-inline";
    const card = botao.closest(".card");

    chamarApi(metodo, url, corpo).then(({ status, ok, texto }) => {
        inline.textContent = `HTTP ${status} ${ok ? "✔" : "✘"}`;
        inline.className = "resultado-inline " + (ok ? "ok" : "erro");
        card.classList.toggle("concluido", ok);
        card.classList.toggle("falhou", !ok);
        if (ok && captura) {
            try { captura(JSON.parse(texto)); } catch { /* corpo não-JSON */ }
            atualizarContexto();
        }
    }).catch((e) => {
        mostrarResposta(0, "Falha de rede — o backend está no ar? (" + e.message + ")");
        inline.textContent = "backend inacessível";
        inline.className = "resultado-inline erro";
        card.classList.remove("concluido");
        card.classList.add("falhou");
    }).finally(() => {
        botao.disabled = false;
    });
}

/* ---------------- renderização ---------------- */

function render() {
    const container = $("#passos");
    let grupoAtual = null;

    PASSOS.forEach((passo, i) => {
        if (passo.grupo !== grupoAtual) {
            grupoAtual = passo.grupo;
            const h = document.createElement("div");
            h.className = "grupo-titulo";
            h.textContent = grupoAtual;
            container.appendChild(h);
        }

        const card = document.createElement("div");
        card.className = "card";
        card.innerHTML = `
            <h3><span class="passo-num"><span>${i + 1}</span></span> ${passo.titulo}</h3>
            <p class="descricao">${passo.descricao}</p>
            <div class="endpoint">
                <span class="metodo ${passo.metodo}">${passo.metodo}</span>
                <span>${passo.path}</span>
            </div>
            ${passo.payload ? `<textarea class="payload" spellcheck="false" rows="${Math.min(
                    JSON.stringify(passo.payload, null, 2).split("\n").length + 1, 16)
                }">${JSON.stringify(passo.payload, null, 2)}</textarea>` : ""}
            <div class="card-acoes">
                <button class="executar">Executar ▶</button>
                <span class="resultado-inline"></span>
            </div>`;
        card.querySelector("button.executar").onclick = () => {
            const textarea = card.querySelector("textarea.payload");
            validarEExecutar(passo.metodo, passo.path,
                textarea ? textarea.value : null,
                card.querySelector(".resultado-inline"),
                card.querySelector("button.executar"),
                passo.captura);
        };
        container.appendChild(card);
    });

    renderModoLivre(container);
}

function renderModoLivre(container) {
    const h = document.createElement("div");
    h.className = "grupo-titulo";
    h.textContent = "8 · Modo livre — monte qualquer requisição";
    container.appendChild(h);

    const card = document.createElement("div");
    card.className = "card";
    card.innerHTML = `
        <h3>🎛️ Requisição manual</h3>
        <p class="descricao">Para tudo que não está no roteiro: escolha um preset ou monte do zero.
           Os placeholders <code>{{servicoId}}</code>, <code>{{pecaId}}</code> e <code>{{osId}}</code>
           funcionam aqui também (edite-os nos chips do topo se precisar).</p>
        <select id="livre-preset" class="livre-select"></select>
        <div class="livre-linha">
            <select id="livre-metodo" class="livre-select metodo-select">
                <option>GET</option><option>POST</option><option>PUT</option><option>DELETE</option>
            </select>
            <input id="livre-path" class="livre-path" spellcheck="false"
                   placeholder="/ordens-servico/{{osId}}" value="/health">
        </div>
        <textarea id="livre-payload" class="payload" spellcheck="false" rows="4"
                  placeholder='payload JSON (vazio = sem corpo)'></textarea>
        <div class="card-acoes">
            <button class="executar">Executar ▶</button>
            <span class="resultado-inline"></span>
        </div>`;

    const selPreset = card.querySelector("#livre-preset");
    PRESETS.forEach((p, i) => {
        const opt = document.createElement("option");
        opt.value = i;
        opt.textContent = p.metodo ? `${p.metodo}  ${p.path}  —  ${p.rotulo}` : p.rotulo;
        selPreset.appendChild(opt);
    });
    selPreset.onchange = () => {
        const p = PRESETS[selPreset.value];
        if (!p.metodo) return;
        card.querySelector("#livre-metodo").value = p.metodo;
        card.querySelector("#livre-path").value = p.path;
        card.querySelector("#livre-payload").value = p.payload ? JSON.stringify(p.payload, null, 2) : "";
    };

    card.querySelector("button.executar").onclick = () => {
        validarEExecutar(
            card.querySelector("#livre-metodo").value,
            card.querySelector("#livre-path").value.trim(),
            card.querySelector("#livre-payload").value,
            card.querySelector(".resultado-inline"),
            card.querySelector("button.executar"),
            null);
    };

    container.appendChild(card);
}

function ligarControles() {
    $("#btn-sair").onclick = sair;
    $("#btn-limpar-contexto").onclick = limparContexto;
    $("#btn-limpar-historico").onclick = limparHistorico;
    $("#chip-auth").onclick = copiarToken;
    $("#chip-servico").onclick = () => editarChip("servicoId", "id do serviço");
    $("#chip-peca").onclick = () => editarChip("pecaId", "id da peça");
    $("#chip-os").onclick = () => editarChip("osId", "id da OS");
}

render();
ligarControles();
atualizarContexto();
