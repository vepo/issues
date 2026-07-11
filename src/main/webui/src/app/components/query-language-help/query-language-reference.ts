export interface QueryFieldHelp {
  name: string;
  operators: string;
  description: string;
}

export interface QueryExample {
  label: string;
  query: string;
}

export const QUERY_LANGUAGE_FIELDS: QueryFieldHelp[] = [
  { name: 'project', operators: '=, !=, ~, IN, NOT IN', description: 'Nome do projeto' },
  { name: 'status', operators: '=, !=, ~', description: 'Nome do status no fluxo de trabalho' },
  { name: 'category', operators: '=, !=, ~', description: 'Nome da categoria' },
  { name: 'assignee', operators: '=, !=, ~, IS EMPTY, IS NOT EMPTY', description: 'E-mail ou nome; use me() ou currentUser()' },
  { name: 'author', operators: '=, !=, ~', description: 'E-mail ou nome; use me() ou currentUser()' },
  { name: 'priority', operators: '=, !=', description: 'LOW, MEDIUM, HIGH ou CRITICAL' },
  { name: 'title', operators: '=, !=, ~', description: 'Título do ticket' },
  { name: 'description', operators: '=, !=, ~, IS EMPTY, IS NOT EMPTY', description: 'Descrição do ticket' },
  { name: 'identifier', operators: '=, !=, ~', description: 'Identificador (ex.: PROJ-1)' },
  { name: 'comment', operators: '~', description: 'Texto nos comentários do ticket' },
  { name: 'phase', operators: '=, !=, ~, IS EMPTY, IS NOT EMPTY', description: 'Nome da fase' },
  { name: 'targetVersion', operators: '=, !=, ~, IS EMPTY, IS NOT EMPTY', description: 'Versão planejada' },
  { name: 'observedVersion', operators: '=, !=, ~, IS EMPTY, IS NOT EMPTY', description: 'Versão entregue' },
  { name: 'dueDate / due', operators: '=, !=, >, <, >=, <=, IS EMPTY, IS NOT EMPTY', description: 'Data de vencimento (ISO-8601, ex.: 2026-08-15)' },
  { name: 'created / createdAt', operators: '=, !=, >, <, >=, <=', description: 'Data de criação (ISO-8601)' },
  { name: 'updated / updatedAt', operators: '=, !=, >, <, >=, <=', description: 'Data de atualização (ISO-8601)' },
  { name: 'finished / finishedAt', operators: '=, !=, >, <, >=, <=, IS EMPTY, IS NOT EMPTY', description: 'Data de conclusão (ISO-8601)' },
  {
    name: 'cf.<chave>',
    operators: '=, !=, ~, >, <, >=, <=, IN, IS EMPTY, IS NOT EMPTY',
    description: 'Campo personalizado pela chave (ex.: cf.sprint = 12). Operadores dependem do tipo do campo.'
  }
];

export const QUERY_LANGUAGE_EXAMPLES: QueryExample[] = [
  {
    label: 'Projeto e status',
    query: 'project = "Issues" AND status = "To Do"'
  },
  {
    label: 'Tickets atribuídos a mim',
    query: 'assignee = me() AND priority = HIGH'
  },
  {
    label: 'Texto no título ou descrição',
    query: 'title ~ "login" OR description ~ "login"'
  },
  {
    label: 'Comentários',
    query: 'comment ~ "revisão"'
  },
  {
    label: 'Campo personalizado',
    query: 'cf.sprint = 12 AND cf.environment = "homolog"'
  },
  {
    label: 'Vários projetos com ordenação',
    query: 'project IN ( "Alpha", "Beta" ) ORDER BY updated DESC'
  }
];
