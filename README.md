<div align="center">

# ✦ NemonicOrbPlugin
### Sistema de Modificação de Itens para Minecraft Java

[![Version](https://img.shields.io/badge/versão-v2.5.0-gold?style=for-the-badge&logo=minecraft&logoColor=white)](.)
[![Server](https://img.shields.io/badge/servidor-NemonicRP-purple?style=for-the-badge)](.)
[![API](https://img.shields.io/badge/Paper%20%2F%20Spigot-1.20+-green?style=for-the-badge&logo=java)](.)
[![MMOItems](https://img.shields.io/badge/MMOItems-compatível-blue?style=for-the-badge)](.)

*Inspirado em Path of Exile 2 · Suporte a Itens Vanilla e MMOItems · Temporada 1*

**[📖 Wiki Completa](../../wiki) · [📩 Contato para aquisição](#-contato)**

</div>

---

## O que é o NemonicOrbPlugin?

O **NemonicOrbPlugin** é um sistema completo de progressão de equipamentos para servidores Minecraft Java de RPG. Ele adiciona uma camada de profundidade estratégica inspirada em *Path of Exile 2*, onde cada item pode ser evoluído, modificado e polido por meio de **Orbs consumíveis**.

Jogadores constroem builds únicas combinando modificadores de categorias distintas, gerenciando raridades e tomando decisões estratégicas sobre quando e como usar cada Orb — o mesmo loop viciante dos ARPGs modernos, dentro do Minecraft.

---

## ✦ Destaques

- **9 tipos de Orbs** com comportamentos distintos e regras claras
- **5 tipos de equipamento** suportados: Arma, Armadura, Acessório, Ferramenta e Escudo
- **50+ modificadores únicos** distribuídos por categorias com pesos balanceados
- **Sistema de Polimento** que amplifica stats em até +47% (com consequências permanentes)
- **3 Mesas de Classe** com mecânicas exclusivas: Alquimista, Ferreiro e Mercador
- **Dano e defesa elemental** em 6 elementos: Fogo, Gelo, Raio, Terra, Água e Vento
- **Valores escaláveis por nível de classe** — quanto mais forte o jogador, melhores os mods
- **Drop de equipamentos não identificados** por mobs, com raridades e bônus por classe
- **Craft com aleatoriedade** — itens craftados na bancada já saem com bônus e encantamentos
- Totalmente configurável via YAML — sem necessidade de recompilar

---

## 🔮 As 9 Orbs

| Orb | Função | Restrição |
|---|---|---|
| 📜 **Pergaminho de Identificação** | Revela os mods ocultos de um item não identificado | Única que funciona em itens Únicos |
| 🫙 **Óleo de Polimento** | +8% em todos os mods por aplicação (máx. 5×). **Trava os mods permanentemente** | Item deve ter mods; irreversível |
| 🔵 **Pedra de Encantamento** | Transforma item Comum → Mágico com 1–2 mods aleatórios | Apenas Tier 0 |
| ➕ **Pedra de Reforço** | Adiciona 1 mod extra a um item Mágico com apenas 1 mod | Apenas Tier 1 com < 2 mods |
| 👑 **Runa Nobre** | Promove Mágico → Raro, adicionando 1 mod extra | Item Mágico com exatamente 2 mods |
| ⚗️ **Pedra de Refinamento** | Transforma Comum → Raro diretamente com 3–6 mods | Apenas Tier 0 |
| ⚡ **Runa de Poder** | Adiciona 1 mod extra a um item Raro com slots disponíveis | Apenas Tier 2 com < 6 mods |
| 🎲 **Moeda da Sorte** | 75% Mágico · 23% Raro · 2% **Único** (mods 50% mais fortes) | Apenas Tier 0 |
| 🧪 **Pedra Corrosiva** | Remove 1 mod **aleatório** do item | Não funciona em itens polidos |

---

## 🏆 Sistema de Raridades (Tiers)

```
Tier 0 — Comum   [cinza]   0 mods  · item base
Tier 1 — Mágico  [azul]    até 2 mods
Tier 2 — Raro    [amarelo] até 6 mods
Tier 3 — Único   [dourado] mods fixos com +50% de poder · só via Moeda da Sorte (2%)
```

**Três rotas de evolução:**

```
SEGURA    Comum ─[Pedra de Encantamento]→ Mágico ─[Runa Nobre]→ Raro
RÁPIDA    Comum ─[Pedra de Refinamento]─────────────────────→ Raro
DA SORTE  Comum ─[Moeda da Sorte]→ Mágico / Raro / ✦ Único ✦
```

---

## 🧬 Modificadores e Categorias

O sistema garante **builds variados**: cada item só pode ter **um mod por categoria**. O sistema sorteia a categoria (com peso) e depois um mod dentro dela.

### Armas
| Categoria | Mods incluídos | Peso |
|---|---|:---:|
| Dano | Ataque, Físico, Mágico, de Arma | 3× |
| Crítico | Chance e Poder de Crit, Skill Crit | 2× |
| Elemental | Fogo, Gelo, Raio, Terra, Água, Vento | 2× |
| Dano de Skill | Habilidades ativas | 2× |
| Velocidade de Ataque | Attack Speed | 1× |
| Lifesteal | Roubo de vida | 1× |
| PvE | Dano contra mobs e bosses | 1× |
| Utilidade | HP, Stamina, Sweeping, Knockback Res | 1× |
| Custo de Magia | Vampirismo de feitiço | 1× |

### Armaduras
| Categoria | Mods incluídos | Peso |
|---|---|:---:|
| Defesa | Armor, Defense | 3× |
| HP | Max HP, Absorção, Regeneração | 3× |
| Mitigação | Block, Dodge, Parry Rating/Power | 2× |
| Tenacidade | Armor Toughness | 2× |
| Resistências | Knockback, Explosão, Queda | 2× |
| Defesa Elemental | 6 elementos (até 3 por item) | 2× |
| Recursos | Mana, Stamina e Regens | 1× |
| CDR | Redução de Cooldown | 1× |
| XP Bônus | Skill EXP, Vanilla EXP | 1× |

### Acessórios, Ferramentas e Escudos
Acessórios: Recursos (Mana/Stamina/Stellium), CDR, HP, Combate, Spell Elemental, Especial.
Ferramentas: Mining Efficiency, Fortune, Durabilidade, Luck, XP.
Escudos: Block Rating/Power, Armor/Defense, HP, Resistências, Defesa Elemental (até 2 por item).

> ⚠️ Todos os tipos suportam **modificadores negativos** para criar tradeoffs estratégicos.

---

## 🔥 Sistema de Polimento

| Aplicações | Ganho total |
|:---:|:---:|
| 1× | +8,0% |
| 2× | +16,6% |
| 3× | +26,0% |
| 4× | +36,0% |
| **5×** | **+46,9%** |

Após o 1º polimento, os mods ficam **permanentemente travados** — Augment, Exalt e Annul não funcionam mais.

---

## 🏗️ Mesas de Classe

### 🧪 Mesa da Transmutação — Alquimista (nível mín. 5/25)
Converte equipamentos em Orbs. Rendimento escala com o ambiente: +1% por estante (máx. +40%) e +5% por jogador próximo (máx. +25%). **Total possível: +65%.**

### 🔨 Mesa do Ferreiro — Ferreiro (nível mín. 5)
Mini-game de forja em **4 estágios sequenciais**: Aquecimento → Modelagem → Resfriamento → Refinamento. A eficiência acumulada determina o breakpoint (0–5) e o bônus de melhoria nos stats (até +20%). Também repara equipamentos com materiais e fabrica Óleo de Polimento.

### 🏪 Mesa do Mercador — Mercador (nível mín. 1–15)
Identifica itens com custo em XP, encanta via livro com chance baseada em eficiência + nível, fabrica Pergaminhos / Poções de Retorno / Pedras de Refinamento e opera uma **rede de Rotas Comerciais** com teletransporte (até 5 waypoints, 3 passageiros, custo 50 Mana).

---

## 📦 Craft com Aleatoriedade

Itens craftados na bancada saem automaticamente com Tier 0, nível registrado, e rolagem de encantamentos + atributos bônus. **5% de Craft Luck** adiciona +10 a +30 níveis extras ao item permanentemente.

---

## ⚔️ Drop de Mobs

```
72% Comum · 22% Mágico · 6% Raro
```
Mercadores têm multiplicador de drop de equipamentos e pergaminhos. Alquimistas têm chance de Pedra Corrosiva. Item-level do drop calculado com variação aleatória.

---

## 📖 Documentação Completa (Wiki)

| Página | Conteúdo |
|---|---|
| [O Que São Orbs?](../../wiki/O-Que-Sao-Orbs) | Conceitos fundamentais |
| [Como Usar uma Orb](../../wiki/Como-Usar-uma-Orb) | Passo a passo, cooldown, feedback |
| [Raridades e Tiers](../../wiki/Raridades-e-Tiers) | Os 4 Tiers e as 3 rotas de evolução |
| [Guia das 9 Orbs](../../wiki/Guia-das-9-Orbs) | Funcionamento detalhado de cada Orb |
| [Modificadores e Categorias](../../wiki/Modificadores-e-Categorias) | Todos os mods por tipo de item |
| [Sistema de Polimento](../../wiki/Sistema-de-Polimento) | Ganho acumulado e estratégia |
| [Identificação de Itens](../../wiki/Identificacao-de-Itens) | Pergaminho, auto-identificação, drops |
| [Craft e Bônus Automáticos](../../wiki/Craft-e-Bonus) | Encantamentos, atributos, Craft Luck |
| [Mesas de Classe](../../wiki/Mesas-de-Classe) | Alquimista, Ferreiro e Mercador em detalhe |
| [Estratégias Recomendadas](../../wiki/Estrategias) | Fluxos para builds eficientes |
| [FAQ](../../wiki/FAQ) | Respostas para as dúvidas mais comuns |

---

## ⚙️ Requisitos Técnicos

| Requisito | Versão |
|---|---|
| Minecraft Java | 1.20+ |
| Server Software | Paper ou Spigot |
| MMOItems | Compatível |
| MMOCore | Compatível |

---

## 📄 Licença

Este plugin é um **software proprietário**. É proibida a redistribuição, cópia, modificação ou uso sem autorização explícita do autor. A licença é concedida individualmente por servidor.

---

## 📩 Contato

- **Discord:** `gepr`
- **Email:** geprworkout@gmail.com
- **LinkedIn:** [Guilherme Elias](https://www.linkedin.com/in/guilherme-elias-8b5b6b255/)

> Licenças por servidor · Suporte técnico incluso · Configuração assistida disponível

---

<div align="center">

*Desenvolvido por **Guilherme Elias (gepr)** · Full Stack Dev @ NemonicRP*

</div>
