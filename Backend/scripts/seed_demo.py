#!/usr/bin/env python3
"""
Popula o EcclesiaControl com dados de demonstração usando só a API REST
(portanto passa por todas as regras: isolamento, PeriodGuard, auditoria).

Uso (API no ar, perfil dev):
    python3 seed_demo.py                      # http://localhost:8080/api/v1
    python3 seed_demo.py --base http://localhost:8080/api/v1 --force

Pré-requisito: DevDataSeeder já rodou (usuários admin/secretaria/tesoureiro @dev.local).
Só usa a biblioteca padrão do Python 3.
"""
import argparse, json, sys, urllib.request, urllib.error
from datetime import date

p = argparse.ArgumentParser()
p.add_argument("--base", default="http://localhost:8080/api/v1")
p.add_argument("--force", action="store_true", help="roda mesmo que a filial já tenha membros")
args = p.parse_args()
BASE = args.base.rstrip("/")
warnings = []


def call(method, path, token=None, body=None, ok=(200, 201, 204), critical=True):
    req = urllib.request.Request(BASE + path, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", token)
    data = json.dumps(body).encode() if body is not None else None
    try:
        with urllib.request.urlopen(req, data) as r:
            raw = r.read().decode()
            return r.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        msg = f"{method} {path} -> {e.code}: {raw[:200]}"
        if critical:
            sys.exit("ERRO: " + msg)
        warnings.append(msg)
        return e.code, None


def login(email, password):
    _, b = call("POST", "/auth/login", body={"email": email, "password": password})
    return "Bearer " + b["accessToken"]


def month_back(n):
    y, m = date.today().year, date.today().month - n
    while m <= 0:
        y, m = y - 1, m + 12
    return y, m


# ------------------------------------------------------------------ logins
admin = login("admin@dev.local", "admin123")
sec = login("secretaria@dev.local", "secretaria123")
tes = login("tesoureiro@dev.local", "tesoureiro123")

_, accounts = call("GET", "/finance/accounts", tes)
rec = [a["id"] for a in accounts if a["code"].startswith("REC") and a["code"] != "REC-004"]
dep = [a["id"] for a in accounts if a["code"].startswith("DESP") and a["code"] != "DESP-003"]
rec001 = next(a["id"] for a in accounts if a["code"] == "REC-001")
if not rec or not dep:
    sys.exit("ERRO: plano de contas sem contas REC-/DESP- utilizáveis")

_, existing = call("GET", "/members?size=1", sec)
if existing["totalElements"] > 0 and not args.force:
    sys.exit("A Filial Teste já tem membros. Use --force para semear mesmo assim.")

# ------------------------------------------------------------------ membros
NOMES = ["Maria das Graças Oliveira", "João Pedro Almeida", "Ana Paula Ferreira", "Carlos Eduardo Lima",
         "Beatriz Souza Costa", "Rafael Gomes Rocha", "Lucia Helena Martins", "Paulo Roberto Dias",
         "Fernanda Cristina Nunes", "Marcos Vinícius Teixeira", "Sandra Regina Barbosa", "Tiago Henrique Araújo"]
membros = []
for i, nome in enumerate(NOMES):
    _, m = call("POST", "/members", sec, {
        "fullName": nome, "birthDate": f"{1960 + i * 4}-0{1 + i % 9}-1{i % 9}",
        "email": f"demo.membro{i + 1}@example.com"})
    membros.append(m["id"])
print(f"{len(membros)} membros criados")

# ------------------------------------------------------------------ lançamentos (4 meses)
for back in (4, 3, 2, 1):
    y, mth = month_back(back)
    for k, dia in enumerate((3, 10, 17, 24)):
        d = f"{y}-{mth:02d}-{dia:02d}"
        call("POST", "/finance/contributions", tes, {
            "accountId": rec001, "amount": 150 + 40 * k + 10 * back, "transactionDate": d,
            "memberId": membros[(k + back) % len(membros)]})
    # uma oferta anônima por mês (sem memberId)
    call("POST", "/finance/contributions", tes, {
        "accountId": rec001, "amount": 85.50, "transactionDate": f"{y}-{mth:02d}-14",
        "anonymous": True}, critical=False)
    for k, conta in enumerate(dep[:3]):
        call("POST", "/finance/transactions", tes, {
            "accountId": conta, "amount": 120 + 60 * k, "transactionDate": f"{y}-{mth:02d}-{8 + 5 * k:02d}",
            "description": f"Despesa de demonstração {k + 1}"})
print("lançamentos criados")

# ------------------------------------------------------------------ fechamento dos 2 meses mais antigos
for back in (4, 3, 2, 1):
    y, mth = month_back(back)
    call("POST", f"/finance/periods/{y}-{mth:02d}/close", tes, critical=False)

# ------------------------------------------------------------------ portal do membro + declarações
tokens = []
creds = []
for mid in membros[:3]:
    _, acc = call("POST", f"/members/{mid}/portal-access", sec)
    tokens.append(login(acc["email"], acc["temporaryPassword"]))
    creds.append((acc["email"], acc["temporaryPassword"]))
decl = []
for n, (tk, valor) in enumerate(zip(tokens * 2, (100, 200, 300, 50, 75, 60))):
    _, r = call("POST", "/me/contribution-declarations", tk, {
        "accountId": rec001, "amount": valor, "declaredDate": date.today().isoformat(),
        "paymentMethod": "PIX", "pixReference": f"E2026DEMO{n:04d}"}, critical=False)
    if r:
        decl.append(r["id"])
if len(decl) >= 3:   # 1 confirmada, 1 rejeitada, o resto fica na fila do tesoureiro
    call("POST", f"/finance/contribution-declarations/{decl[0]}/confirm", tes, critical=False)
    call("POST", f"/finance/contribution-declarations/{decl[1]}/reject", tes,
         {"reason": "Não localizei o PIX no extrato"}, critical=False)
print(f"{len(decl)} declarações criadas (1 confirmada, 1 rejeitada, {max(len(decl) - 2, 0)} pendentes)")

# ------------------------------------------------------------------ contas a pagar
hoje = date.today()
ny, nm = (hoje.year + (hoje.month // 12), hoje.month % 12 + 1)
pagar = [("Companhia de Energia (Demo)", 480.00, hoje.replace(day=min(hoje.day + 5, 28)).isoformat(), "FIXA"),
         ("Papelaria Central (Demo)", 235.90, f"{ny}-{nm:02d}-10", "FIXA"),
         ("Reforma do telhado (Demo)", 3200.00, f"{ny}-{nm:02d}-20", "EXTRAORDINARIA")]
ids_pagar = []
for nome, valor, venc, tipo in pagar:
    _, r = call("POST", "/finance/payables", tes, {"accountId": dep[0], "payeeName": nome, "amount": valor,
                                                    "dueDate": venc, "expenseKind": tipo}, critical=False)
    if r:
        ids_pagar.append(r["id"])
if ids_pagar:   # baixa parcial da primeira conta (campos do corpo ainda não confirmados)
    call("POST", f"/finance/payables/{ids_pagar[0]}/settlements", tes,
         {"amount": 200.00, "transactionDate": hoje.isoformat()}, critical=False)
print(f"{len(ids_pagar)} contas a pagar criadas")

# ------------------------------------------------------------------ segunda filial (isolamento + RN-009)
_, nf = call("POST", "/filiais", admin, {"name": "Filial Norte (Demo)", "city": "Goiânia", "state": "GO"},
             critical=False)
if nf:
    fid = nf["id"]
    _, u = call("POST", "/users", admin, {"email": "tesoureiro.norte@dev.local", "fullName": "Tesoureiro Norte (Demo)",
                                           "initialPassword": "Demo@2026norte"}, critical=False)
    if u:
        call("POST", f"/users/{u['id']}/roles", admin, {"role": "TESOUREIRO_FILIAL", "filialId": fid}, critical=False)
        tn = login("tesoureiro.norte@dev.local", "Demo@2026norte")
        y, mth = month_back(1)
        for k in range(3):
            call("POST", "/finance/contributions", tn, {"accountId": rec001, "amount": 200 + 25 * k,
                                                         "transactionDate": f"{y}-{mth:02d}-{5 + 7 * k:02d}",
                                                         "anonymous": True},
                 critical=False)
        print("Filial Norte criada com 3 lançamentos e nenhum mês fechado (fora do dashboard oficial)")

print("\nMembros com acesso ao portal (senha temporária, só para dev):")
for em, pw in creds:
    print(f"  {em} | {pw}")
print("\nPronto. Entre com tesoureiro@dev.local / tesoureiro123 (Filial Teste) ou admin@dev.local / admin123.")
if warnings:
    print("\nAVISOS (passos opcionais que a API recusou; veja se é regra ou diferença de campo):")
    for w in warnings:
        print(" -", w)
