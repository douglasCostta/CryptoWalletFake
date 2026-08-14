# Resumo: Fluxo Wallet (Buy/Sell) — para sua conferência

**Nada foi commitado.** Tudo abaixo está apenas no seu diretório de trabalho, sem stage, sem commit, sem push — exatamente como pedido. Este documento existe para você revisar antes de decidir o que commitar.

---

## 1. O que foi pedido

Implementar o fluxo de Wallet (Wallet → Buy/Sell → Transaction Completed) no app `CryptoWalletFake`, seguindo o design do mockup fornecido, em Kotlin/Jetpack Compose com arquitetura MVVM.

## 2. Descoberta inicial importante

O app já existia (`CryptoWalletFake`), mas só tinha uma tela de dashboard de cotações lendo a API pública da CoinGecko (somente leitura — preço, gráfico). **Não existe backend de saldo/transações.** Por isso, o saldo da carteira e as compras/vendas são simulados 100% localmente (persistidos no dispositivo via DataStore), com os preços das moedas continuando a vir ao vivo da CoinGecko.

Antes de programar, passamos por uma etapa de brainstorming onde você aprovou estas decisões:

| Decisão | Escolha |
|---|---|
| Persistência do saldo | Local (DataStore), não em memória |
| Injeção de dependência | Manual, sem Hilt/Koin (igual ao resto do projeto) |
| Navegação | Novo `NavHost` em `MainActivity`, substituindo a tela de teste antiga |
| "USD" no mockup | Tratado como stablecoin (USDC) negociável, igual BTC/ETH/BNB |
| `BuySellViewModel` | Um único ViewModel compartilhado entre Buy e Sell (parametrizado) |

A especificação completa está em [`docs/superpowers/specs/2026-08-07-wallet-buy-sell-design.md`](specs/2026-08-07-wallet-buy-sell-design.md) e o plano de implementação passo a passo em [`docs/superpowers/plans/2026-08-07-wallet-buy-sell.md`](plans/2026-08-07-wallet-buy-sell.md).

## 3. Como foi executado

O plano foi dividido em 14 tarefas, cada uma implementada por um agente, e **revisada por dois outros agentes independentes** antes de seguir para a próxima (um verificando se bateu com a especificação, outro verificando qualidade/bugs do código). Isso é o que permitiu pegar os bugs listados na seção 5 — não foram encontrados por "sorte", foram encontrados porque cada pedaço de código foi lido e questionado antes de ser aceito.

## 4. Arquivos criados e modificados

### Novos (18 arquivos de produção/teste)

**Modelo de dados**
- `app/src/main/java/com/cryptowallet/model/WalletModels.kt` — `TransactionType`, `WalletHolding`, `WalletState`, `CoinBalance`, `WalletBalance`, `TransactionRequest`, `TransactionResult`

**Persistência local**
- `app/src/main/java/com/cryptowallet/data/local/WalletDataStore.kt` — extensão de DataStore no `Context`
- `app/src/main/java/com/cryptowallet/data/local/WalletLocalDataSource.kt` — leitura/escrita/atualização atômica do saldo salvo

**Repositório**
- `app/src/main/java/com/cryptowallet/data/repository/WalletRepository.kt` — interface
- `app/src/main/java/com/cryptowallet/data/repository/WalletRepositoryImpl.kt` — implementação (combina preços da CoinGecko com o saldo local)

**ViewModels**
- `app/src/main/java/com/cryptowallet/viewmodel/WalletViewModel.kt`
- `app/src/main/java/com/cryptowallet/viewmodel/BuySellViewModel.kt`

**Navegação**
- `app/src/main/java/com/cryptowallet/navigation/Routes.kt`
- `app/src/main/java/com/cryptowallet/navigation/CryptoWalletNavHost.kt`

**Telas (Compose)**
- `app/src/main/java/com/cryptowallet/view/WalletScreen.kt`
- `app/src/main/java/com/cryptowallet/view/BuySellScreen.kt`
- `app/src/main/java/com/cryptowallet/view/TransactionSuccessScreen.kt`
- `app/src/main/java/com/cryptowallet/view/components/WalletComponents.kt` — botão com gradiente e abas em formato pílula (visual do mockup)

**Testes automatizados** (26 testes no total)
- `app/src/test/java/com/cryptowallet/MainDispatcherRule.kt`
- `app/src/test/java/com/cryptowallet/fakes/FakeCoinGeckoApi.kt`
- `app/src/test/java/com/cryptowallet/fakes/FakeWalletRepository.kt`
- `app/src/test/java/com/cryptowallet/data/local/WalletLocalDataSourceTest.kt`
- `app/src/test/java/com/cryptowallet/data/repository/CoinGeckoRepositoryTest.kt`
- `app/src/test/java/com/cryptowallet/data/repository/WalletRepositoryImplTest.kt`
- `app/src/test/java/com/cryptowallet/viewmodel/WalletViewModelTest.kt`
- `app/src/test/java/com/cryptowallet/viewmodel/BuySellViewModelTest.kt`

### Modificados (7 arquivos)

- `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts` — novas dependências: DataStore Preferences, kotlinx-serialization (para rotas de navegação type-safe), kotlinx-coroutines-test, lifecycle-runtime-compose
- `app/src/main/java/com/cryptowallet/data/remote/service/CoinGeckoService.kt` e `.../data/repository/CoinGeckoRepository.kt` — adicionado suporte a moeda BRL e busca de várias moedas por id (e corrigido um bug, ver seção 5)
- `app/src/main/java/com/cryptowallet/MainActivity.kt` — agora monta o `WalletRepository` e usa o novo `NavHost` como tela inicial, no lugar da tela de teste antiga
- `app/src/main/java/com/cryptowallet/ui/theme/Color.kt` e `.../ui/theme/Theme.kt` — paleta e tema escuro/laranja do mockup

**Nada foi apagado.** `TesteScreen.kt`, `TesteViewModel.kt` e `DashboardViewModel.kt` continuam no projeto, só não são mais o ponto de entrada do app.

## 5. Bugs encontrados e corrigidos durante as revisões

Isso é o que mais vale a pena você conferir. Cada item foi achado por uma revisão de código independente, não fazia parte do pedido original:

1. **Bug pré-existente (não era da wallet):** `CoinGeckoRepository`/`CoinGeckoService` tinham uma troca de posição de parâmetros que fazia o `DashboardViewModel` sempre mostrar os dados do Bitcoin, não importa qual moeda fosse selecionada. Corrigido.
2. **Corrida de dados (race condition):** duas operações de compra/venda simultâneas podiam se sobrescrever no armazenamento local. Corrigido com uma atualização atômica (`updateWalletState`).
3. **Sem proteção contra dado corrompido:** se o JSON salvo no dispositivo estivesse corrompido ou incompleto, o app quebraria ao abrir. Corrigido com fallback para o saldo inicial.
4. **🔴 Exploit crítico:** era possível digitar um valor **negativo** no campo "Amount in R$" durante uma compra e isso **adicionava dinheiro** à carteira sem descontar nada — quebrava a regra básica de conservação de valor da carteira. Corrigido com validação tanto na tela quanto no repositório (duas camadas, para não depender só da tela).
5. **Perda silenciosa de dinheiro:** se o id da moeda não existisse na carteira, o dinheiro era descontado mas a moeda nunca era creditada, sem erro nenhum. Corrigido para falhar de forma visível.
6. **Tela não atualizava sozinha:** ao voltar da tela de sucesso para a Wallet, o saldo ficava desatualizado até fechar e abrir o app de novo — quebrava exatamente o fluxo principal pedido. Corrigido.
7. **Chamadas de rede duplicadas (achado ao testar no emulador):** cada vez que a tela Wallet atualizava, o app fazia 3 chamadas simultâneas para a CoinGecko, sendo 2 delas idênticas/redundantes — isso ajudava a estourar o limite de requisições da API gratuita (erro "HTTP 429"). Corrigido: agora são só 2 chamadas de rede por atualização (a terceira informação, o saldo total, é calculada localmente a partir do que as outras duas já trouxeram). Também adicionei um link "Tentar novamente" na tela Wallet para quando um erro desses acontecer.
8. **Outros ajustes menores:** teclado numérico nos campos de valor, aceitar vírgula como separador decimal (padrão brasileiro), botão "Confirm" não travava mais silenciosamente com campo vazio, ícones das moedas que estavam faltando na tela Wallet.

## 6. Testes automatizados

```
./gradlew :app:testDebugUnitTest
```
**26 testes, 0 falhas.** Cobrem: persistência local, repositório (saldo, compra, venda, casos de erro), e os dois ViewModels (carregamento, seleção de moeda, conversão de valores, confirmação).

```
./gradlew :app:assembleDebug
```
**Build completo, sem erros.**

## 7. Tema visual

Depois de você mandar os mockups de novo, apliquei o visual escuro com gradiente laranja/âmbar:
- Fundo escuro com brilho (glow) sutil no canto, como no mockup
- Botões "Buy"/"Sell"/"Confirm"/"Back to Wallet" com gradiente laranja→âmbar em formato pílula
- Abas "Your coins"/"All coins" como controle segmentado em pílula
- Cards escuros com borda sutil para saldo e linhas de moeda

## 8. O que **não** foi possível verificar

Tentei abrir o emulador Android neste ambiente para testar visualmente e navegar pelo fluxo inteiro (comprar, vender, ver o resultado), mas o emulador não consegue rodar aqui (sem suporte a virtualização neste ambiente sandbox). Então:

- ✅ Testado: compilação, build completo, os 26 testes automatizados.
- ❌ **Não testado por mim:** a aparência real na tela e o fluxo de toque (tocar em Buy, digitar valor, confirmar, ver a tela de sucesso, voltar e ver o saldo atualizado).

Você já resolveu o problema de armazenamento do emulador — quando conseguir rodar, seria bom conferir esse fluxo completo pelo menos uma vez.

## 9. Próximos passos

Nada foi commitado. Quando você validar (visualmente e/ou lendo o diff), me avise e eu preparo o(s) commit(s) — posso separar em commits menores (ex: "add wallet models/repository", "add wallet screens", "apply wallet theme") ou fazer um único commit, como preferir.
