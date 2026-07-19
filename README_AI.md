# 🧠 Contexto de Arquitetura e Engenharia do Projeto (Estudo/SaaS)
> **Este arquivo foi criado especificamente para alimentar assistentes de Inteligência Artificial (LLMs)**, provendo um entendimento completo sobre as regras de negócio, stack técnica, decisões de design, requisitos funcionais, não funcionais e caminhos para evolução do projeto.

---

## 1. Visão Geral do Sistema
O **Study Management Platform (StudyFlow)** é uma plataforma SaaS de alta performance voltada para a gestão de estudos, produtividade e aprendizagem ativa baseada em evidências. Seus principais pilares de experiência de usuário são:
1. **Planejamento e Metas**: Definição de objetivos de carga horária por matéria.
2. **Ciclo de Estudo**: Registro e acompanhamento de sessões de estudo ativas.
3. **Memorização Espaçada**: Criação de Flashcards baseados no *Leitner System* com agendamento inteligente.
4. **Centralização de Materiais**: Upload de PDFs de aula com leitor embutido e anotações por página.
5. **Editor Notion-style**: Criação de resumos de estudo ricos e limpos.

---

## 2. Stack Tecnológica

### Backend (Java)
*   **Core**: Java 21 + Spring Boot 3.2.4.
*   **Segurança**: Spring Security + JWT (JSON Web Tokens).
*   **Persistência**: Spring Data JPA + Hibernate.
*   **Banco de Dados**: MySQL (Produção/Desenvolvimento local) + Flyway para migrações de esquema.
*   **Testes**: JUnit 5 + Mockito + Banco H2 *in-memory* (configurado em modo de compatibilidade PostgreSQL).
*   **Auxiliares**: Lombok (Gerações boilerplates com segurança), Jsoup (Sanitizador de HTML).

### Frontend (React)
*   **Core**: React 19 + TypeScript + Vite.
*   **Roteamento**: React Router DOM v7.
*   **Gerenciamento de Estado**: Zustand (Estado global leve e reativo).
*   **Comunicação API**: Axios (Cliente HTTP customizado).
*   **Visualização de Dados**: Recharts (Gráficos interativos de evolução semanal/foco).
*   **Estilização**: Tailwind CSS.

---

## 3. Arquitetura de Software (Modular Monolith)
O backend foi reestruturado de uma arquitetura baseada em camadas técnicas (`controller/service/repository`) para uma **Arquitetura Baseada em Recursos/Funcionalidades (Modular Monolith / Package-by-Feature)**. 

### Estrutura de Pacotes (`src/main/java/com/studyplatform`)
Cada pasta funcional encapsula suas próprias Controllers, Services, Repositories, Mappers, DTOs e Classes de Domínio:
*   `com.studyplatform.auth`: Fluxos de cadastro, login e DTOs de segurança.
*   `com.studyplatform.user`: Perfil do usuário e persistência correspondente.
*   `com.studyplatform.subject`: Gestão de matérias (Subject) e Value Objects de visualização.
*   `com.studyplatform.goal`: Definição de metas de horas e recalculadores automáticos de progresso.
*   `com.studyplatform.session`: Registro de sessões de estudo ativas.
*   `com.studyplatform.summary`: Editor de resumos com higienizador HTML embutido.
*   `com.studyplatform.flashcard`: Algoritmo Leitner de repetição espaçada e revisões.
*   `com.studyplatform.file`: Upload físico de arquivos PDF e anotações associadas às páginas.
*   `com.studyplatform.ai`: Gerador inteligente de flashcards (via Gemini API / mock fallback).
*   `com.studyplatform.shared`: Pacote transversal contendo configurações globais, filtros de segurança (`JwtAuthenticationFilter`), tratamento global de exceções e filtros transversais (`RateLimitingFilter`).

---

## 4. Requisitos Funcionais (RF) e Regras de Negócio

### RF01: Gestão de Matérias (Subject)
*   Cada matéria possui um nome, descrição opcional e um **Value Object `Color`** que valida o formato hexadecimal (ex: `#FF5733`).
*   Um usuário não pode possuir duas matérias com o mesmo nome.

### RF02: Metas de Estudo (Goal)
*   Permite definir horas de estudo desejadas para uma matéria (ou meta geral) em um período de tempo (`startDate` a `endDate`).
*   O progresso da meta é calculado em horas decimais de forma assíncrona/dinâmica a partir do tempo acumulado nas sessões de estudo pertencentes àquele intervalo.
*   A entidade expõe a lógica de conclusão através de `getCompletionPercentage()`.

### RF03: Sessões de Estudo (StudySession)
*   Registra a data da sessão, descrição e duração em minutos.
*   Sempre que uma sessão é criada, editada ou removida, as metas de estudo correspondentes àquele período são automaticamente recalculadas no banco de dados.

### RF04: Resumos Notion-style (Summary)
*   Editor rico que aceita elementos de marcação rica.
*   O conteúdo salvo é obrigatoriamente higienizado no backend contra XSS.

### RF05: Flashcards (Leitner System)
*   Algoritmo de memorização ativa. Cada cartão avança ou retrocede entre 5 caixas (`LeitnerBox`):
    *   **Acerto Fácil/Bom**: Avança 1 nível de caixa (máximo 5).
    *   **Erro**: Reinicia para a Caixa 1.
*   O intervalo para próxima revisão é calculado pelo Value Object `LeitnerBox`:
    *   *Caixa 1*: Próxima revisão em 1 dia.
    *   *Caixa 2*: Próxima revisão em 3 dias.
    *   *Caixa 3*: Próxima revisão em 7 dias.
    *   *Caixa 4*: Próxima revisão em 14 dias.
    *   *Caixa 5*: Próxima revisão em 30 dias.

### RF06: Central de PDFs e Anotações
*   Upload físico de PDFs limitado e estruturado na pasta `uploads` do servidor.
*   Usuários podem salvar anotações de texto livre indexadas por número de página e arquivo.

---

## 5. Requisitos Não Funcionais (RNF)

### RNF01: Isolamento de Dados por Tenant
*   Todas as consultas SQL filtram os dados com base no usuário autenticado no contexto de segurança. Nenhum usuário pode visualizar, alterar ou deletar registros de outros.

### RNF02: Proteção contra Brute Force e Flood (Rate Limiting)
*   Filtro `RateLimitingFilter` com algoritmo *Token Bucket* em memória:
    *   **Rotas Sensíveis** (Login, Cadastro, Geração de IA): Limite restrito a **20 requisições/minuto por IP**.
    *   **Rotas Gerais**: Limite de **200 requisições/minuto por IP**.
    *   Retorno HTTP **429 Too Many Requests** se violado.

### RNF03: Segurança Contra XSS e Clickjacking
*   Higienização HTML no backend via Jsoup (`Safelist.relaxed()`).
*   Headers Nginx no frontend: `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Content-Security-Policy` restritivo.

### RNF04: Segurança com Lombok e Hibernate
*   Substituição de anotações `@Data` por `@Getter`/`@Setter` individuais com `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` apontando exclusivamente para a chave primária (`@Id`).
*   Relacionamentos Bidirecionais anotados com `@ToString.Exclude` nas chaves estrangeiras para mitigar estouros de pilha (`StackOverflowError`).

### RNF05: Paginação da API
*   Todos os endpoints de listagem de recursos utilizam paginação via `Pageable` e `PageRequest` com ordenação reversa por ID (`id DESC`), mitigando sobrecarga de memória na aplicação.

---

## 6. Esquema do Banco de Dados e Migrações (Flyway)
*   `V1__create_tables.sql`: Define a criação estrutural de todas as tabelas (utilizando sintaxe compatível ANSI/PostgreSQL).
*   `V2__add_indexes.sql`: Adiciona índices de performance nas tabelas:
    *   Índices nas chaves estrangeiras (`user_id`, `subject_id`, `file_id`).
    *   Índices compostos para consultas frequentes de listas ordenadas (ex: `idx_flashcards_user_next_review` indexando `user_id` e `next_review_date`).

---

## 7. Áreas para Evolução e Sugestões de Melhorias (Para IA Analisar)
Quando estiver sugerindo melhorias, compare as seguintes abordagens:
1.  **Cache L1/L2**: Substituição de buscas repetitivas em sessões ativas e matérias por uma camada de cache (ex: Redis ou Caffeine Cache).
2.  **Mensageria**: Mudar o recalculador de metas em `StudySessionService` para um padrão orientado a eventos (Event-Driven) usando Spring ApplicationEvents (síncrono/assíncrono) ou RabbitMQ.
3.  **Auditoria JPA**: Substituição das anotações manuais de datas por `@CreatedDate` e `@LastModifiedDate` usando Auditing do Spring Data JPA.
4.  **Autenticação**: Suporte a OAuth2/Social Login (Google/GitHub) integrado ao Spring Security atual.
5.  **Testes de Integração**: Adicionar Testcontainers para validar migrações Flyway e consultas MySQL contra uma instância Docker real de banco de dados nos testes.
