# Changelog

## Vídeo 002 [Commit f9347db](https://github.com/vepo/issues/commit/f9347db01d7bd9325d7807657d827c305e9cedc2)

1. Projeto inicial criado com controller para "Change"
2. Classe de teste criada

## Pós Vídeo 002 [Commit b11afe3](https://github.com/vepo/issues/commit/b11afe37d7ec7e2d308c4c091a6566d5524b8fa3)

1. Mudança de pacote `com.seuprojeto` para `io.vepo.issues`
2. Distribuição das classes nos pacotes por dominio e não por funcionalidade
3. Correção de import errado. O método `greaterThanOrEqualTo` fazia parte da classe `org.hamcrest.Matchers` e não `org.hamcrest.CoreMatchers`.
4. Adiciona `io.quarkus:quarkus-rest-jackson` como dependência.
5. Adiciona `@Transactional` ao método `ChangeController.create`
6. Substitui `"application/json"` por `MediaType.APPLICATION_JSON`
7. Comentando testes que não foram implementados.

## Video 003 [Commit a246846](https://github.com/vepo/issues/commit/a24684693c2d11a06ce368eb3a0fbd1737004e18)

1. Adicionando projeto Angular com Quinoa para build.

## Video 004 [Commit 5ec6761](https://github.com/vepo/issues/commit/5ec67614da2ab79a305203710c0b0aac7ca28fe0)

1. Modelando tabelas usando JPA (`User` e `Ticket`)

## Video 005 [Commit 601e396](https://github.com/vepo/issues/commit/601e396b33a798c097245f149e579484447dcfb8)

1. Criando APIs para criação de Tickets

## Video 006 [Commit 2a6caf3](https://github.com/vepo/issues/commit/2a6caf33abb04bbe09483ab6e775fe683fefaf82)

1. Criação do serviço de Workflow 

## Pós Video 006 [Commit ff0b092](https://github.com/vepo/issues/commit/ff0b092a5b7781ebca98fc870648613ab5e92ccd)

1. Separação de pacotes para os domínios Ticket, Project e Workflow.
2. Validação de arquitetura pelo ArchUnit
3. Implementação dos serviços essenciais para criação do Ticket. CRUD de Projeto e Workflow.

## Video 007 [Commit 6260a76](https://github.com/vepo/issues/commit/6260a76a09c4770d557f8936842aead31e1b4adf)

1. Criação da página Kanban `/kanban`.

## Pós Vídeo 007

1. Melhoria no código de popular base de testes
2. Criação do endpoint de consultar projeto por id, tickets e status por projeto.
3. Criação de Services e Resolver no Angular para ler dados enquanto navega, evitando loading
4. Reomoção do código no compomente do Kanban e implementação da lógica no HTML

## Vídeo 008 [Commit df60c36](https://github.com/vepo/issues/commit/df60c36ecccfcb2dec5b764f7d720dbc3d5cd3a4)

1. Melhoria visual e implementação do drag-n-drop

## Pós Vídeo 008

1. Renomear Stage para Status
2. Fazer página de busca funcionar.
    1. Adicionar Status a busca
    2. Mecanismo de query não estava funcionando
3. Fazer funcionar a página de Kanban


## Video 009 [Commit 346240b](https://github.com/vepo/issues/commit/346240b4bfc711019a878109673490fd1204cf6d)

1. Implementar formulário de criação de ticket

## Pós Vídeo 009

1. Mudança de identidade visual [Commit 6ba9901](https://github.com/vepo/issues/commit/6ba99017c6660b09705481688b2a5e378e30d797)
2. Criar carga de dados para desenvolvimento [Commit fb59f1a](https://github.com/vepo/issues/commit/fb59f1a5eab213d3ccdfa0641ab26a285b7e0b29)

## Vídeo 010 

1. Criação do mecanismo de autenticação

## Vídeo 011

1. Criação do menu logout

## Pós Vídeo 011

1. Mudança de pacote `io.vepo` para `dev.vepo`
2. Revisão da interface
3. Criação de menus faltantes

## Live 2

1. [Implementação do Log de Mudanças](/changelog/HISTORY_LOG_IMPLEMENTATION.md)
2. [Implementação do suporte a comentários](/changelog/COMMENTS_IMPLEMENTATION.md)
3. [Melhorias no suporte a comentários](/changelog/COMMENTS_IMPROVEMENTS.md)