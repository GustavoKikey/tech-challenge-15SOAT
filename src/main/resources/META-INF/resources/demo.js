/*
 * Painel de demonstração das APIs — Tech Challenge Fase 3.
 *
 * Sem framework: os passos são declarados no array PASSOS e renderizados como
 * cards. Cada execução mostra método + endpoint + payload resolvido + resposta,
 * e captura ids (token, serviçoId, peçaId, osId) para os passos seguintes via
 * placeholders {{...}} nos paths e payloads.
 *
 * Além do roteiro guiado há o MODO LIVRE (monte qualquer requisição, com
 * presets de todos os endpoints) e controles de sessão/contexto no cabeçalho.
 *
 * Dois emissores de token, um painel
 * ---------------------------------
 * O funcionário se autentica na própria aplicação, com usuário e senha. O
 * cliente se autentica por CPF numa Function serverless, atrás do API Gateway
 * — outro domínio, outro processo. O painel guarda os dois tokens separados e
 * envia o que o passo pedir: passo com `perfil: "cliente"` manda o token do
 * cliente, os demais mandam o do funcionário.
 *
 * Guardar um token só, sobrescrevendo, faria os passos se atropelarem — e o
 * 401 resultante pareceria falha de autenticação em vez de troca de contexto.
 */

const estado = {
    accessToken: null,      // funcionário — emitido pela aplicação
    tokenCliente: null,     // cliente — emitido pela Function, via API Gateway
    clienteNome: null,
    servicoId: null,
    pecaId: null,
    osId: null,
    osOutroCliente: null    // OS de outro cliente, para provar o isolamento
};

/* Preenchido no boot por /config-demo, que repassa o que o deploy leu do
   Parameter Store. Vazio significa que não há Gateway provisionado. */
let API_GATEWAY = "";

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


/* ---------------- fase 3: autenticação do cliente por CPF ----------------
 *
 * Estes passos saem do host da aplicação: o path começa com "@gateway", e o
 * motor troca esse prefixo pelo endereço do API Gateway. Os passos seguintes
 * voltam a ser relativos — a área do cliente é servida pelo cluster.
 */
const PASSOS_CLIENTE = [
    {
        grupo: "6 · Fase 3 — Cliente por CPF (API Gateway + Function)",
        titulo: "CPF inválido — a Function recusa antes do banco",
        descricao: "A validação do dígito verificador acontece na Function, antes de qualquer consulta. CPF malformado nem chega ao Postgres. Esperado: 400.",
        metodo: "POST",
        path: "@gateway/auth/cliente",
        perfil: "publico",
        payload: { cpf: "111.111.111-11" },
        esperado: 400
    },
    {
        grupo: "6 · Fase 3 — Cliente por CPF (API Gateway + Function)",
        titulo: "CPF válido, cliente não cadastrado",
        descricao: "Esperado: 401 — e a resposta é idêntica à de um cliente inativo, de propósito. Respostas diferentes transformariam este endpoint num consultor de cadastro: daria para descobrir quem é cliente da oficina pelo formato do erro.",
        metodo: "POST",
        path: "@gateway/auth/cliente",
        perfil: "publico",
        payload: { cpf: "111.444.777-35" },
        esperado: 401
    },
    {
        grupo: "6 · Fase 3 — Cliente por CPF (API Gateway + Function)",
        titulo: "Autenticar cliente cadastrado",
        descricao: "A Function consulta a base, confirma que o cliente existe e está ativo, e assina um JWT RS256 com validade de 30 minutos — bem menor que as 8 horas do token de funcionário. A aplicação não sabe autenticar cliente: ela só confere a assinatura com a chave pública.",
        metodo: "POST",
        path: "@gateway/auth/cliente",
        perfil: "publico",
        payload: { cpf: "529.982.247-25" },
        captura: (body) => {
            estado.tokenCliente = body.accessToken;
            estado.clienteNome = body.cliente ? body.cliente.nome : null;
        }
    },
    {
        grupo: "7 · Fase 3 — Área do cliente (rotas protegidas)",
        titulo: "As ordens de serviço do cliente autenticado",
        descricao: "Repare que não há id de cliente na URL. Ele vem do claim sub do token — a rota devolve as OS de quem está autenticado, e não de quem for pedido.",
        metodo: "GET",
        path: "/cliente/ordens-servico",
        perfil: "cliente",
        captura: (body) => {
            if (Array.isArray(body) && body.length) estado.osId = body[0].id;
        }
    },
    {
        grupo: "7 · Fase 3 — Área do cliente (rotas protegidas)",
        titulo: "Tentar a OS de OUTRO cliente",
        descricao: "Cole no chip “OS de outro” o id de uma ordem em andamento que pertença a outro cliente. Esperado: 403 — existe, e não é sua. A checagem de dono acontece dentro da transação, junto da leitura.",
        metodo: "GET",
        path: "/cliente/ordens-servico/{{osOutroCliente}}",
        perfil: "cliente",
        esperado: 403
    },
    {
        grupo: "7 · Fase 3 — Área do cliente (rotas protegidas)",
        titulo: "Sem token — negado por padrão",
        descricao: "A aplicação nega por padrão: rota que não declara quem pode acessar não fica aberta, fica fechada. Esperado: 401.",
        metodo: "GET",
        path: "/cliente/ordens-servico",
        perfil: "publico",
        esperado: 401
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
    const cli = $("#chip-cliente");
    if (cli) {
        cli.textContent = estado.tokenCliente
            ? `👤 ${estado.clienteNome || "cliente"} autenticado`
            : "👤 cliente não autenticado";
        cli.classList.toggle("ativo", !!estado.tokenCliente);
        cli.title = estado.tokenCliente
            ? "Token emitido pela Function, via API Gateway"
            : "Execute o passo de autenticação por CPF";
    }
    marcarChip("#chip-servico", "serviçoId", estado.servicoId);
    marcarChip("#chip-peca", "peçaId", estado.pecaId);
    marcarChip("#chip-os", "OS", estado.osId);
    marcarChip("#chip-os-outro", "OS de outro", estado.osOutroCliente);
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
    estado.tokenCliente = null;
    estado.clienteNome = null;
    atualizarContexto();
    mostrarResposta(200, "Sessão local encerrada — os dois tokens foram descartados.\n" +
        "Os próximos passos protegidos responderão 401 até novo login.");
    $("#resp-status").textContent = "logout";
    $("#resp-status").className = "status-badge ok";
}

function limparContexto() {
    estado.servicoId = null;
    estado.pecaId = null;
    estado.osId = null;
    estado.osOutroCliente = null;
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

/* Qual token vai no cabeçalho depende do perfil do passo:
   "cliente" usa o emitido pela Function; "publico" não manda nenhum (é o que
   prova o 401 da rota protegida); o padrão é o token de funcionário. */
function autorizacao(perfil) {
    if (perfil === "publico") return {};
    const token = perfil === "cliente" ? estado.tokenCliente : estado.accessToken;
    return token ? { "Authorization": "Bearer " + token } : {};
}

async function chamarApi(metodo, url, corpo, perfil) {
    mostrarRequisicao(metodo, url, corpo);
    const resp = await fetch(url, {
        method: metodo,
        headers: {
            ...(corpo ? { "Content-Type": "application/json" } : {}),
            ...autorizacao(perfil)
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

/* "@gateway/auth/cliente" -> "https://xxx.execute-api.../auth/cliente".
   Path sem o prefixo continua relativo, servido pelo mesmo host do painel. */
function resolverBase(path) {
    if (!path.startsWith("@gateway")) return path;
    return API_GATEWAY + path.slice("@gateway".length);
}

function validarEExecutar(metodo, pathBruto, textoPayload, inline, botao, captura, perfil, esperado) {
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

    const url = resolverBase(resolver(pathBruto));
    if (url.startsWith("@gateway")) {
        inline.textContent = "endereço do API Gateway não disponível (/config-demo veio vazio)";
        inline.className = "resultado-inline erro";
        return;
    }
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

    chamarApi(metodo, url, corpo, perfil).then(({ status, ok, texto }) => {
        // Passo com `esperado` demonstra uma recusa: 401 e 403 SÃO o resultado
        // correto ali. Marcá-los como falha ensinaria a ler vermelho como erro
        // justamente onde o vermelho é a prova.
        const sucesso = esperado ? status === esperado : ok;
        inline.textContent = `HTTP ${status} ${sucesso ? "✔" : "✘"}`
            + (esperado ? ` (esperado ${esperado})` : "");
        inline.className = "resultado-inline " + (sucesso ? "ok" : "erro");
        card.classList.toggle("concluido", sucesso);
        card.classList.toggle("falhou", !sucesso);
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
                passo.captura, passo.perfil, passo.esperado);
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
            null, null, null);
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
    const outro = $("#chip-os-outro");
    if (outro) outro.onclick = () => editarChip("osOutroCliente", "id da OS de outro cliente");
}

/* O painel sobe funcionando mesmo sem Gateway: os passos de CPF simplesmente
   não são renderizados, em vez de virarem botões que falhariam. */
async function carregarConfiguracao() {
    try {
        const resp = await fetch("/config-demo");
        if (!resp.ok) return;
        const cfg = await resp.json();
        API_GATEWAY = (cfg.apiGatewayUrl || "").replace(/\/+$/, "");
        const rotulo = $("#tag-ambiente");
        if (rotulo && cfg.ambiente) rotulo.textContent = cfg.ambiente;
    } catch {
        /* sem /config-demo (versão anterior da aplicação): segue sem Gateway */
    }
}

carregarConfiguracao().then(() => {
    if (API_GATEWAY) PASSOS.push(...PASSOS_CLIENTE);
    render();
    ligarControles();
    atualizarContexto();
    if (!API_GATEWAY) {
        const aviso = $("#aviso-gateway");
        if (aviso) aviso.hidden = false;
    }
});
