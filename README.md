# Projeto de Gestão e Simulação de Tráfego com SNMP

Este projeto tem como objetivo desenvolver um sistema distribuído baseado no protocolo **SNMP**, composto por quatro módulos principais — **SC**, **SD**, **CMC** e **SSFR** — que comunicam entre si para gerir e monitorizar o estado de um sistema de tráfego.

---

##  SC – Sistema Central

### 📘 Descrição
O **SC (Sistema Central)** é o núcleo do sistema.  
É responsável por manter toda a informação de tráfego, como o estado dos semáforos, sensores e atuadores.  
A MIB (Management Information Base) é gerida neste módulo e contém todos os objetos e respetivos valores.  
O **SCAgent** atua como interface SNMP, permitindo que módulos externos (ex. CMC) consultem ou alterem dados através do protocolo SNMP.

### ✅ Requisitos Funcionais
| Nº | Requisito | Estado |
|----|------------|--------|
| 1 | Implementar a classe **MIBobjects** para representar objetos da MIB | 🟢 Concluído |
| 2 | Implementar o **Agent** para gerir pedidos SNMP | 🟢 Concluído |
| 3 | Criar **Estrutura de dados** para armazenar e atualizar valores da **MIB** | 🟢 Concluído |
| 4 | Integrar o acesso de leitura e escrita entre **SD** e **SSFR** | 🟢 Concluído |

---

##  SD – Sistema de Decisão

### 📘 Descrição
O **SD** é responsável por analisar os dados do tráfego (ex. número de veículos) e determinar o comportamento do sistema,  
como o tempo dos semáforos e possíveis ajustes automáticos de fluxo.

### ✅ Requisitos Funcionais
| Nº | Requisito | Estado |
|----|------------|--------|
| 1 | Implementar algoritmo de decisão com base nos dados do tráfego | 🟢 Concluído |
| 2 | Comunicar com o **SC** para alterar parâmetros da MIB | 🟢 Concluído |
| 3 | Garantir que as decisões são registadas e atualizadas em tempo real | 🟢 Concluído |

---

##  CMC – Consola de Monitorização e Controlo

### 📘 Descrição
A **CMC** faz a parte do **"Gestor** é a interface do utilizador que permite monitorizar e controlar o sistema de tráfego.  
Deve utilizar o protocolo SNMP para ler e alterar valores do **SC**, apresentando a informação de forma gráfica ou textual.

### ✅ Requisitos Funcionais
| Nº | Requisito | Estado |
|----|------------|--------|
| 1 | Implementar interface SNMP Manager para comunicação com o SC | 🟢 Concluído |
| 2 | Criar interface de utilizador (CLI ou GUI) para visualização do estado | 🟢 Concluído |
| 3 | Permitir envio de comandos para alterar valores da MIB (ex. estado do semáforo) | 🟢 Concluído |

---

##  SSFR – Simulador de Fluxo Rodoviário

### 📘 Descrição
O **SSFR** é o módulo responsável por simular o fluxo de veículos nas estradas e cruzamentos.  
Os resultados da simulação são atualizados na MIB através do **SC**, permitindo ao **SD** reagir às mudanças.

### ✅ Requisitos Funcionais
| Nº | Requisito | Estado |
|----|------------|--------|
| 1 | Simular a movimentação de veículos em tempo real | 🟢 Concluído |
| 2 | Atualizar automaticamente os valores de tráfego na MIB | 🟢 Concluído |
| 3 | Comunicar com o SC para envio de dados de simulação | 🟢 Concluído |

---

## 🗓️ Progresso e Atualizações
### 12/11/2025
- **MIB** criado pelo SNMP4J agentpro



### 19/10/2025
- Iniciado o desenvolvimento da classe **ScAgent**,responsavel por processar os pedidos.
- Iniciado o densenvolvimento da classe **MIB**,para armazenar as informações.
- Testar sistema com um CMC simple.


### 16/10/2025
- Iniciado o desenvolvimento da classe **MIBobjects** 
- Iniciado o estudo e compreensão da biblioteca **SNMP4J**

---

---

###  Legenda de estados
- 🟢 **Concluído**
- 🟡 **Em progresso**
- ⬜ **Não iniciado**
- 🔴 **Com problemas**

---

 *Última atualização:* _19/10/2025_
