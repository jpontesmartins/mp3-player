# PRD — ofy-015 — Shell Tauri e backend embarcado

> Levantamento retroativo (v1.6.0): descreve o comportamento **já implementado** no código. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Visão Geral

Aplicativo desktop Windows **ovelhafy** (Tauri v2) que abre a interface React e inicia, de forma transparente, o backend Java empacotado com JRE próprio, encerrando-o ao sair.

## Objetivos

- Instalação única sem exigir Java/Node.
- Ciclo de vida do backend atrelado ao app.

## Histórias de Usuário

- Como usuário, quero abrir o app e usá-lo sem iniciar nada manualmente.
- Como usuário, quero que o processo Java feche junto com o app.

## Funcionalidades Principais

1. **RF-015.1 Janela:** 1400x800 (mínimo 1000x600), título `ovelhafy`.
2. **RF-015.2 Spawn do backend:** `resources/jre/bin/java.exe -jar resources/backend.jar` com argumentos de porta/log/cache; sem janela de console (`CREATE_NO_WINDOW`); stdout/stderr em `<app_log_dir>/backend.log`.
3. **RF-015.3 Encerramento:** o processo é finalizado em `RunEvent::Exit`.
4. **RF-015.4 Abrir no Explorer:** plugin `opener` (`revealItemInDir`).
5. **Critérios de aceite:** (a) após instalar, o app carrega a biblioteca; (b) fechar o app não deixa `java.exe` órfão.

## Experiência do Usuário

- Sem tela de carregamento do backend; o frontend tenta `localhost:8111` conforme as requisições.

## Restrições Técnicas de Alto Nível

- Somente Windows no spawn (`java.exe`, `CREATE_NO_WINDOW`); CSP desabilitado (`null`); permissões `core:default` e `opener:default`.

## Fora de Escopo

- Outros SOs, comandos Tauri/IPC próprios, atualização automática.

## Questões em Aberto

- Em desenvolvimento (`tauri dev`) o backend **não** é iniciado pelo shell; deve ser executado à parte.
- Falhas ao iniciar o backend são silenciosas (sem aviso ao usuário) `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Porta `8111` fixa no frontend; conflito de porta não é tratado.
