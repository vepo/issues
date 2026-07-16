DO $$
    DECLARE
    todo_id        INTEGER;
    progress_id    INTEGER;
    blocked_id     INTEGER;
    done_id        INTEGER;
    agile_id       INTEGER;
    feature_id     INTEGER;
    bug_id         INTEGER;
    user_cto_id    INTEGER;
    proj_issues_id INTEGER;
    ver_1_0_0_id   INTEGER;
    ver_1_1_0_id   INTEGER;
    phase_mvp_id   INTEGER;
    phase_next_id  INTEGER;
    open_id        INTEGER;
    info_id        INTEGER;
    analyse_id     INTEGER;
    valid_id       INTEGER;
    support_id     INTEGER;
    cancel_id      INTEGER;
    cf_env_id      INTEGER;
    cf_sprint_id   INTEGER;
    cf_notes_id    INTEGER;
    cf_points_id   INTEGER;
    cf_blocked_id  INTEGER;
    cf_env_homolog INTEGER;
BEGIN
    -- Usuário com apenas a role PROJECT_MANAGER
    INSERT INTO tb_users (username, name, email, encoded_password, roles, ui_locale) VALUES 
                         ('proj-leader', 'Project Lead', 'project_lead@issues.ui', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==', '{PROJECT_MANAGER}', 'pt');

    -- Usuário com apenas a role USER
    INSERT INTO tb_users (username, name, email, encoded_password, roles, ui_locale) VALUES 
                         ('junior', 'Junior Developer', 'junior_dev@issues.ui', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==', '{USER}', 'pt');

    -- Combinação ADMIN + PROJECT_MANAGER
    INSERT INTO tb_users (username, name, email, encoded_password, roles, ui_locale) VALUES 
                         ('project-boss', 'Director of Projects', 'director_projects@issues.ui', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==', '{ADMIN,PROJECT_MANAGER}', 'pt');

    -- Combinação ADMIN + USER
    INSERT INTO tb_users (username, name, email, encoded_password, roles, ui_locale) VALUES 
                         ('tech-lead', 'Tech Lead', 'tech_lead@issues.ui', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==', '{ADMIN,USER}', 'pt');

    -- Combinação PROJECT_MANAGER + USER
    INSERT INTO tb_users (username, name, email, encoded_password, roles, ui_locale) VALUES 
                         ('senior', 'Senior Developer', 'senior_dev@issues.ui', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==', '{PROJECT_MANAGER,USER}', 'pt');

    -- Combinação ADMIN + PROJECT_MANAGER + USER (Super Usuário)
    INSERT INTO tb_users (username, name, email, encoded_password, roles, ui_locale) VALUES 
                         ('cto-boss', 'Chief Technology Officer', 'cto@issues.ui', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==', '{ADMIN,PROJECT_MANAGER,USER}', 'pt')
                         RETURNING ID INTO user_cto_id;

    -- Usuário sem roles (se aplicável)
    INSERT INTO tb_users (username, name, email, encoded_password, roles, ui_locale) VALUES 
                         ('guest-user', 'Guest User', 'guest@issues.ui', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==', '{}', 'pt');


    INSERT INTO tb_categories (name, color) VALUES ('Feature', '#01172F') RETURNING id INTO feature_id;
    INSERT INTO tb_categories (name, color) VALUES ('Bug', '#C62828') RETURNING id INTO bug_id;

    INSERT INTO tb_workflow_status (name) VALUES ('TODO')        RETURNING id INTO todo_id;
    INSERT INTO tb_workflow_status (name) VALUES ('IN_PROGRESS') RETURNING id INTO progress_id;
    INSERT INTO tb_workflow_status (name) VALUES ('BLOCKED')     RETURNING id INTO blocked_id;
    INSERT INTO tb_workflow_status (name) VALUES ('DONE')        RETURNING id INTO done_id;

    INSERT INTO tb_workflows (name, start_id, phase_start_id) VALUES ('Agile', todo_id, progress_id) RETURNING id INTO agile_id;

    INSERT INTO tb_workflow_statuses (workflow_id, status_id) VALUES (agile_id, todo_id);
    INSERT INTO tb_workflow_statuses (workflow_id, status_id) VALUES (agile_id, progress_id);
    INSERT INTO tb_workflow_statuses (workflow_id, status_id) VALUES (agile_id, blocked_id);
    INSERT INTO tb_workflow_statuses (workflow_id, status_id) VALUES (agile_id, done_id);

    INSERT INTO tb_workflow_transitions (workflow_id, from_id, to_id) VALUES (agile_id, todo_id,     progress_id);
    INSERT INTO tb_workflow_transitions (workflow_id, from_id, to_id) VALUES (agile_id, progress_id, blocked_id);
    INSERT INTO tb_workflow_transitions (workflow_id, from_id, to_id) VALUES (agile_id, blocked_id,  progress_id);
    INSERT INTO tb_workflow_transitions (workflow_id, from_id, to_id) VALUES (agile_id, progress_id, done_id);

    INSERT INTO tb_workflow_finish_statuses (workflow_id, status_id, outcome) VALUES (agile_id, done_id, 'DONE');

    INSERT INTO tb_workflow_status (name) VALUES ('OPEN')                RETURNING id INTO open_id;
    INSERT INTO tb_workflow_status (name) VALUES ('REQUEST_INFORMATION') RETURNING id INTO info_id;
    INSERT INTO tb_workflow_status (name) VALUES ('VALIDATING')          RETURNING id INTO valid_id;
    INSERT INTO tb_workflow_status (name) VALUES ('ANALYSING')           RETURNING id INTO analyse_id;
    INSERT INTO tb_workflow_status (name) VALUES ('CANCEL')              RETURNING id INTO cancel_id;

    INSERT INTO tb_workflows (name, start_id) VALUES ('Support', open_id) RETURNING id INTO support_id;

    INSERT INTO tb_workflow_statuses (workflow_id, status_id) VALUES (support_id, open_id);
    INSERT INTO tb_workflow_statuses (workflow_id, status_id) VALUES (support_id, progress_id);
    INSERT INTO tb_workflow_statuses (workflow_id, status_id) VALUES (support_id, blocked_id);
    INSERT INTO tb_workflow_statuses (workflow_id, status_id) VALUES (support_id, valid_id);
    INSERT INTO tb_workflow_statuses (workflow_id, status_id) VALUES (support_id, info_id);
    INSERT INTO tb_workflow_statuses (workflow_id, status_id) VALUES (support_id, analyse_id);
    INSERT INTO tb_workflow_statuses (workflow_id, status_id) VALUES (support_id, done_id);

    INSERT INTO tb_workflow_transitions (workflow_id, from_id, to_id) VALUES (support_id, open_id,     analyse_id);
    INSERT INTO tb_workflow_transitions (workflow_id, from_id, to_id) VALUES (support_id, analyse_id,  progress_id);
    INSERT INTO tb_workflow_transitions (workflow_id, from_id, to_id) VALUES (support_id, analyse_id,  info_id);
    INSERT INTO tb_workflow_transitions (workflow_id, from_id, to_id) VALUES (support_id, analyse_id,  cancel_id);
    INSERT INTO tb_workflow_transitions (workflow_id, from_id, to_id) VALUES (support_id, info_id,     analyse_id);
    INSERT INTO tb_workflow_transitions (workflow_id, from_id, to_id) VALUES (support_id, info_id,     cancel_id);
    INSERT INTO tb_workflow_transitions (workflow_id, from_id, to_id) VALUES (support_id, info_id,     progress_id);
    INSERT INTO tb_workflow_transitions (workflow_id, from_id, to_id) VALUES (support_id, progress_id, blocked_id);
    INSERT INTO tb_workflow_transitions (workflow_id, from_id, to_id) VALUES (support_id, blocked_id,  progress_id);
    INSERT INTO tb_workflow_transitions (workflow_id, from_id, to_id) VALUES (support_id, progress_id, valid_id);
    INSERT INTO tb_workflow_transitions (workflow_id, from_id, to_id) VALUES (support_id, valid_id,    done_id);

    INSERT INTO tb_workflow_finish_statuses (workflow_id, status_id, outcome) VALUES (support_id, done_id, 'DONE');
    INSERT INTO tb_workflow_finish_statuses (workflow_id, status_id, outcome) VALUES (support_id, cancel_id, 'CANCELED');

    INSERT INTO tb_projects (name, description, prefix, workflow_id, owner_id, security_level,
                             ticket_template_enabled, ticket_template_title,
                             ticket_template_description, ticket_template_category_id, ticket_template_priority)
    VALUES ('Issues', 'MVP Issues', 'ISS', agile_id, user_cto_id, 'INTERNAL',
            TRUE, 'New work item',
            'Describe the change or defect using the sections below.',
            bug_id, 'MEDIUM')
    RETURNING id INTO proj_issues_id;

    INSERT INTO tb_project_members (project_id, user_id)
    SELECT proj_issues_id, u.id FROM tb_users u
    WHERE u.username IN ('cto-boss', 'senior', 'junior', 'proj-leader', 'project-boss', 'tech-lead');

    -- Sample Private / Public projects for security-level exploration
    INSERT INTO tb_projects (name, description, prefix, workflow_id, owner_id, security_level)
    VALUES ('Private Demo', 'Membros e admin apenas', 'PRV', agile_id, user_cto_id, 'PRIVATE');

    INSERT INTO tb_projects (name, description, prefix, workflow_id, owner_id, security_level)
    VALUES ('Public Demo', 'Leitura anônima permitida', 'PUB', agile_id, user_cto_id, 'PUBLIC');

    INSERT INTO tb_versions (project_id, label, description)
    VALUES (proj_issues_id, '1.0.0', 'MVP inicial')
    RETURNING id INTO ver_1_0_0_id;

    INSERT INTO tb_versions (project_id, label, description)
    VALUES (proj_issues_id, '1.1.0', 'Melhorias e estabilização')
    RETURNING id INTO ver_1_1_0_id;

    INSERT INTO tb_phases (project_id, name, objective, status, deliverable_version_id, created_at, completed_at)
    VALUES (proj_issues_id, 'MVP 1.0', 'Entregar o MVP inicial do Issues', 'COMPLETED', ver_1_0_0_id, NOW() - INTERVAL '30 days', NOW() - INTERVAL '7 days')
    RETURNING id INTO phase_mvp_id;

    INSERT INTO tb_phase_deliverables (phase_id, sort_order, text) VALUES
        (phase_mvp_id, 0, 'Autenticação e usuários'),
        (phase_mvp_id, 1, 'Tickets e workflow Kanban');

    INSERT INTO tb_phases (project_id, name, objective, status, start_date, end_date, deliverable_version_id, created_at)
    VALUES (proj_issues_id, 'Release 1.1', 'Melhorias e importação CSV', 'ACTIVE',
            CURRENT_DATE - INTERVAL '14 days', CURRENT_DATE + INTERVAL '14 days', ver_1_1_0_id, NOW() - INTERVAL '3 days')
    RETURNING id INTO phase_next_id;

    INSERT INTO tb_phase_deliverables (phase_id, sort_order, text) VALUES
        (phase_next_id, 0, 'Importação CSV de tickets'),
        (phase_next_id, 1, 'Gestão de fases e versões');

    UPDATE tb_projects
    SET phase_template_objective = 'Entregas incrementais do produto Issues'
    WHERE id = proj_issues_id;

    INSERT INTO tb_project_phase_deliverable_templates (project_id, sort_order, text) VALUES
        (proj_issues_id, 0, 'Funcionalidades de ticket e workflow'),
        (proj_issues_id, 1, 'Planejamento por fases e versões');

    -- Custom fields (project-owned)
    INSERT INTO tb_custom_fields (field_key, label, field_type, required, enabled, project_id, sort_order)
    VALUES ('environment', 'Ambiente', 'ENUM', FALSE, TRUE, proj_issues_id, 0)
    RETURNING id INTO cf_env_id;

    INSERT INTO tb_custom_field_enum_options (custom_field_id, option_value, label, sort_order) VALUES
        (cf_env_id, 'dev', 'Desenvolvimento', 0),
        (cf_env_id, 'homolog', 'Homologação', 1),
        (cf_env_id, 'prod', 'Produção', 2);

    SELECT id INTO cf_env_homolog FROM tb_custom_field_enum_options
    WHERE custom_field_id = cf_env_id AND option_value = 'homolog';

    INSERT INTO tb_custom_fields (field_key, label, field_type, required, enabled, project_id, integer_min, integer_max, sort_order)
    VALUES ('sprint', 'Sprint', 'INTEGER', FALSE, TRUE, proj_issues_id, 1, 99, 1)
    RETURNING id INTO cf_sprint_id;

    INSERT INTO tb_custom_fields (field_key, label, field_type, required, enabled, project_id, sort_order)
    VALUES ('customer_notes', 'Notas do cliente', 'TEXT', FALSE, TRUE, proj_issues_id, 2)
    RETURNING id INTO cf_notes_id;

    -- Custom fields (workflow-owned on Agile)
    INSERT INTO tb_custom_fields (field_key, label, field_type, required, enabled, workflow_id, integer_min, integer_max, sort_order)
    VALUES ('story_points', 'Story points', 'INTEGER', FALSE, TRUE, agile_id, 1, 21, 0)
    RETURNING id INTO cf_points_id;

    INSERT INTO tb_custom_field_status_required (custom_field_id, status_id)
    VALUES (cf_points_id, done_id);

    INSERT INTO tb_custom_fields (field_key, label, field_type, required, enabled, workflow_id, sort_order)
    VALUES ('needs_review', 'Precisa de revisão', 'BOOLEAN', FALSE, TRUE, agile_id, 1)
    RETURNING id INTO cf_blocked_id;

    INSERT INTO tb_project_ticket_template_custom_values (project_id, custom_field_id, integer_value, enum_option_id)
    VALUES (proj_issues_id, cf_sprint_id, 1, NULL);

    INSERT INTO tb_project_ticket_template_custom_values (project_id, custom_field_id, enum_option_id)
    VALUES (proj_issues_id, cf_env_id, cf_env_homolog);

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-001',
                            'Setup do Ambiente de Desenvolvimento', 
                            'Configurar JDK 17+, Maven, Node.js e IDE (ex.: VS Code) nas máquinas do time. Documentar requisitos.',
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            done_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-002',
                            'Setup do Projeto Quarkus', 
                            'Criar projeto base com Quarkus CLI (ou Maven), configurar dependências (Hibernate, RESTEasy, JWT) e estrutura de pacotes.', 
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            done_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-003',
                            'Setup do Projeto Angular', 
                            'Criar projeto Angular com Angular CLI, configurar rotas básicas, HttpClient e UI Kit (ex.: Angular Material).', 
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            done_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-004',
                            'Configuração da Build Integrada', 
                            'Criar scripts (Maven/NPM) para build conjunta (ex.: `mvn clean install` + `ng build`). Configurar proxy no Angular para API do Quarkus.', 
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            todo_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-005',
                            'Modelagem do Banco de Dados', 
                            'Criar tabelas `changes`, `workflow_stages`, `users` e relações no Quarkus (usando Hibernate ORM).', 
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            done_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-006',
                            'API REST (Quarkus)', 
                            'Implementar endpoints para CRUD de mudanças (`/api/changes`).', 
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            done_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-007',
                            'Serviço de Workflow', 
                            'Lógica para transição entre estágios (ex.: `PATCH /api/changes/{id}/move`).', 
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            done_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-008',
                            'Frontend: Listagem Kanban', 
                            'Página Angular com colunas arrastáveis (usando `ngx-dnd` ou similar).', 
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            done_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-009',
                            'Frontend: Formulário de Mudança', 
                            'Componente Angular com campos dinâmicos (JSON configurável).', 
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            done_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-010',
                            'Autenticação Básica', 
                            'Login simples (JWT + Quarkus Security).', 
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            done_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-011',
                            'Notificações', 
                            'Enviar e-mails/alerts quando mudança muda de estágio (ex.: SMTP).', 
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            done_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-012',
                            'Dashboard', 
                            'Gráfico simples (ex.: Chart.js) com status das mudanças.', 
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            done_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-013',
                            'Logs de Histórico', 
                            'Registrar alterações no BD (ex.: "Usuário X aprovou em DD/MM").', 
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            done_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-014',
                            'Deploy Inicial', 
                            'Subir backend (Quarkus) e front (Angular) em servidor de teste (ex.: Docker Compose ou Heroku).', 
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            todo_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-015',
                            'Gestão de Usuários', 
                            'Tela de cadastro/edição', 
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            done_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-016',
                            'Alterar/Recuperar Senha', 
                            'Tela de alteração/recuperação de senha usando e-mail como forma de recuperação', 
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            progress_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, created_at, updated_at) VALUES 
                           ('ISS-017',
                            'Gestão de Projetos', 
                            'Tela de cadastro/edição', 
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            done_id,
                            NOW(), 
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, ticket_type, created_at, updated_at) VALUES
                           ('ISS-018',
                            'Vínculos entre tickets',
                            'Épico de demonstração: relacionamentos tipados e subtarefas sob um épico.',
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            todo_id,
                            'EPIC',
                            NOW(),
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, ticket_type, created_at, updated_at) VALUES
                           ('ISS-019',
                            'Schema e entidade de vínculos',
                            'Subtarefa do épico ISS-018: tabelas e entidades JPA.',
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            done_id,
                            'TASK',
                            NOW(),
                            NOW());

    INSERT INTO tb_tickets (identifier, title, description, author_id, project_id, category_id, status_id, ticket_type, created_at, updated_at) VALUES
                           ('ISS-020',
                            'API de vínculos',
                            'Subtarefa do épico ISS-018: endpoints de listagem, criação e remoção.',
                            user_cto_id,
                            proj_issues_id,
                            feature_id,
                            progress_id,
                            'TASK',
                            NOW(),
                            NOW());

    UPDATE tb_tickets SET due_date = CURRENT_DATE + INTERVAL '14 days' WHERE identifier = 'ISS-014';
    UPDATE tb_tickets SET due_date = CURRENT_DATE + INTERVAL '7 days' WHERE identifier = 'ISS-016';

    INSERT INTO tb_ticket_links (source_ticket_id, target_ticket_id, link_type, created_at, created_by)
    SELECT child.id, epic.id, 'CHILD_OF', NOW(), user_cto_id
    FROM tb_tickets child
    CROSS JOIN tb_tickets epic
    WHERE child.identifier = 'ISS-019' AND epic.identifier = 'ISS-018';

    INSERT INTO tb_ticket_links (source_ticket_id, target_ticket_id, link_type, created_at, created_by)
    SELECT child.id, epic.id, 'CHILD_OF', NOW(), user_cto_id
    FROM tb_tickets child
    CROSS JOIN tb_tickets epic
    WHERE child.identifier = 'ISS-020' AND epic.identifier = 'ISS-018';

    INSERT INTO tb_ticket_links (source_ticket_id, target_ticket_id, link_type, created_at, created_by)
    SELECT source.id, target.id, 'BLOCKS', NOW(), user_cto_id
    FROM tb_tickets source
    CROSS JOIN tb_tickets target
    WHERE source.identifier = 'ISS-016' AND target.identifier = 'ISS-004';

    INSERT INTO tb_ticket_links (source_ticket_id, target_ticket_id, link_type, created_at, created_by)
    SELECT source.id, target.id, 'RELATES_TO', NOW(), user_cto_id
    FROM tb_tickets source
    CROSS JOIN tb_tickets target
    WHERE source.identifier = 'ISS-018' AND target.identifier = 'ISS-004';

    INSERT INTO tb_ticket_history (action, field, old_value, new_value, timestamp, ticket_id, user_id)
    SELECT 'CREATED', NULL, NULL, NULL, NOW() - INTERVAL '7 days', t.id, user_cto_id
    FROM tb_tickets t WHERE t.identifier = 'ISS-001';

    INSERT INTO tb_ticket_history (action, field, old_value, new_value, timestamp, ticket_id, user_id)
    SELECT 'STATUS_CHANGED', 'status', 'Todo', 'Done', NOW() - INTERVAL '5 days', t.id, user_cto_id
    FROM tb_tickets t WHERE t.identifier = 'ISS-001';

    INSERT INTO tb_ticket_history (action, field, old_value, new_value, timestamp, ticket_id, user_id)
    SELECT 'CREATED', NULL, NULL, NULL, NOW() - INTERVAL '2 days', t.id, user_cto_id
    FROM tb_tickets t WHERE t.identifier = 'ISS-004';

    INSERT INTO tb_ticket_history (action, field, old_value, new_value, timestamp, ticket_id, user_id)
    SELECT 'FIELD_CHANGED', 'title', 'Configuração da Build', 'Configuração da Build Integrada', NOW() - INTERVAL '1 day', t.id, user_cto_id
    FROM tb_tickets t WHERE t.identifier = 'ISS-004';

    UPDATE tb_tickets
    SET target_version_id = ver_1_1_0_id,
        phase_id = phase_next_id
    WHERE identifier = 'ISS-004';

    UPDATE tb_tickets
    SET phase_id = phase_next_id
    WHERE identifier = 'ISS-016';

    UPDATE tb_tickets
    SET story_points = 5
    WHERE identifier IN ('ISS-004', 'ISS-016');

    UPDATE tb_tickets
    SET story_points = 3
    WHERE identifier IN ('ISS-008', 'ISS-009');

    UPDATE tb_tickets
    SET story_points = 8
    WHERE identifier = 'ISS-010';

    UPDATE tb_tickets
    SET observed_version_id = ver_1_0_0_id,
        target_version_id = ver_1_0_0_id,
        finished_at = NOW() - INTERVAL '3 days'
    WHERE identifier IN ('ISS-001', 'ISS-002', 'ISS-003', 'ISS-005', 'ISS-006', 'ISS-015', 'ISS-017', 'ISS-019');

    UPDATE tb_tickets t
    SET backlog_rank = sub.rn
    FROM (
        SELECT id, (ROW_NUMBER() OVER (PARTITION BY project_id ORDER BY id))::int AS rn
        FROM tb_tickets
    ) sub
    WHERE t.id = sub.id;

    INSERT INTO tb_ticket_custom_field_values (ticket_id, custom_field_id, integer_value, enum_option_id)
    SELECT t.id, cf_sprint_id, 1, NULL
    FROM tb_tickets t WHERE t.identifier = 'ISS-004';

    INSERT INTO tb_ticket_custom_field_values (ticket_id, custom_field_id, enum_option_id)
    SELECT t.id, cf_env_id, cf_env_homolog
    FROM tb_tickets t WHERE t.identifier = 'ISS-004';

    INSERT INTO tb_ticket_custom_field_values (ticket_id, custom_field_id, integer_value)
    SELECT t.id, cf_points_id, 5
    FROM tb_tickets t WHERE t.identifier = 'ISS-016';

    INSERT INTO tb_ticket_custom_field_values (ticket_id, custom_field_id, boolean_value)
    SELECT t.id, cf_blocked_id, TRUE
    FROM tb_tickets t WHERE t.identifier = 'ISS-016';

    INSERT INTO tb_saved_queries (slug, name, query_text, show_at_home, owner_id, created_at, updated_at)
    SELECT 'dev-open-tickets', 'Tickets abertos', 'status = "TODO"', true, id, NOW(), NOW()
    FROM tb_users WHERE email = 'junior_dev@issues.ui';

    INSERT INTO tb_project_git_repositories (project_id, remote_url, provider, default_branch, webhook_secret, created_at, updated_at)
    VALUES (proj_issues_id, 'https://github.com/vepo/issues', 'GITHUB', 'main', 'dev-webhook-secret-not-for-prod', NOW(), NOW());

    INSERT INTO tb_ticket_commits (ticket_id, project_id, sha, message, author_name, author_email, matched_user_id, committed_at, commit_url, created_at)
    SELECT t.id, proj_issues_id, 'a1b2c3d4e5f67890',
           'fix(auth): redirect after login (ISS-003)',
           'Junior Developer', 'junior_dev@issues.ui', junior.id,
           NOW() - INTERVAL '2 days',
           'https://github.com/vepo/issues/commit/a1b2c3d4e5f67890',
           NOW() - INTERVAL '2 days'
    FROM tb_tickets t
    CROSS JOIN tb_users junior
    WHERE t.identifier = 'ISS-003' AND junior.email = 'junior_dev@issues.ui';
END $$;