# Onboarding hints

**Feature version:** 2  
**Status:** done  
**Requested:** 2026-07-03

## Summary

Dismissible **context hints** on key screens to close ARCHITECTURE §13 gap (in-app onboarding). One short PT-BR banner per screen; dismissal persisted in browser `localStorage`.

## Wireframe

| Screen | Hint (PT-BR) |
|--------|----------------|
| `/` Home | Seu hub pessoal: tickets abertos nos seus projetos, atribuições e atividade recente. |
| Kanban | Arraste cards entre colunas para mover o status do ticket. |
| `/search/advanced` | Use a linguagem de consulta ou abra a ajuda abaixo para ver campos e operadores. |

Layout: `.context-hint` banner below `.page-header`; **Fechar** button on the right.

## Feature questions

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Which screens? | answered | Home, Kanban, Advanced search |
| FQ2 | Persistence? | answered | `localStorage` key `issues.hint.dismissed.{hintId}` |
| FQ3 | Re-show UI? | answered | No — dev clears localStorage |
| FQ4 | Copy language? | answered | PT-BR |

## Architecture

| Layer | Change |
|-------|--------|
| Component | `ContextHintComponent` — inputs `hintId`, `message`; dismiss hides banner |
| Styles | `.context-hint` in `styles.scss`; gallery entry |
| Integration | `home`, `kanban`, `advanced-search` templates |

## Changelog

### Context hints — 2026-07-03

**Version:** 1  
**Status:** done

**Development approval:** approved 2026-07-03 — tasks: T13–T18

#### Tasks

| ID | Task | Done |
|----|------|------|
| T13 | Feature doc (this file) | ☑ |
| T14 | `ContextHintComponent` + `.context-hint` gallery | ☑ |
| T15 | Integrate Home, Kanban, Advanced search | ☑ |
| T16 | Component spec + localStorage test | ☑ |
| T17 | ARCHITECTURE §13, ui-nielsen-audit | ☑ |
| T18 | ui-design-system TC3 manual smoke | ☑ |

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `context-hint.component.spec.ts` | T14, T16 | ☑ |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Hints on three screens match **Wireframe** | Wireframe | ☑ |
| FC2 | Dismiss persists in localStorage | FQ2 | ☑ |
| FC3 | ARCHITECTURE §13 onboarding gap closed | Docs | ☑ |

**Implementation notes:** `ContextHintComponent`; integrated on home, kanban, advanced-search; gallery §9.0.

### Catalog rows for context hints — 2026-07-11

**Version:** 2  
**Status:** done

**Description:** Docs-only — feature-catalog Home / Kanban / Advanced search Steps note dismissible context hints. Source: [feature-catalog-review](../reports/feature-catalog-review-1-11-07-2026-16-27-54.md).

**Development approval:** n/a — docs-only
