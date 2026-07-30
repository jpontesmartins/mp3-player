# Skill: Frontend Minimalista & Monochrome (Tauri + React + Vite + TS)

## Objetivo
Garantir interfaces limpas, coesas e elegantes utilizando estritamente uma paleta monocromática, além de assegurar arquitetura de componentes escalável e integração fluida com o Tauri.

## 1. Diretriz Visual: Paleta Monocromática Estrita
- **Proibido:** Uso de cores primárias coloridas (azuis, verdes chamativos, vermelhos de destaque) em elementos estruturais da UI, botões principais ou fundos.
- **Permitido exclusivamente:** Preto (`#000000` / `#111111`), Branco (`#FFFFFF`) e escalas de Cinza (`#1A1A1A`, `#666666`, `#E5E5E5`, `#F5F5F5`).
- **Exceção Funcional:** Cores semânticas de status (sucesso/erro) devem ser usadas com extremo comedimento apenas em ícones pontuais ou feedbacks de formulários.

## 2. Arquitetura e Padrões (React + Vite + TS)
- **TypeScript Estrito:** Zero uso de `any`. Uso obrigatório de tipos explícitos, interfaces e type guards.
- **Componentização:** Padrão limpo e modular. Regras de negócio desacopladas da UI através de Custom Hooks.
- **Gerenciamento de Estado:** Context API leve ou estados locais customizados; evite dependências excessivas para aplicações desktop focadas em performance.

## 3. Especificidades do Tauri
- Comunicação assíncrona blindada com o backend em Rust via `invoke()` tipado.
- Cuidado com o consumo de memória e otimização de bundle (Vite) para manter a aplicação extremamente rápida e leve.
- Respeito a padrões de janelas nativas e áreas de arraste (`data-tauri-drag-region`).