

# 📚 MangaVW API

Live Demo (Produção): [Acesse o Swagger UI no Render](https://mangavw.onrender.com/swagger-ui/index.html)

A MangaVW API é uma plataforma RESTful robusta desenvolvida para gerenciar um acervo completo de mangás. O sistema abrange desde os metadados editoriais e categorizações das obras até a estrutura granular de leitura (capítulos e páginas), além de contar com integração de dados via API externa.

Desenvolvido como requisito para o curso de TSI no Senac, o projeto aplica modelagem de dados avançada, paginação, HATEOAS e boas práticas de arquitetura REST.

---

Stack Tecnológica

* **Java 17 / Spring Boot 3**
* **Spring Data JPA & Hibernate**: Gestão de persistência e mapeamento objeto-relacional complexo.
* **Spring HATEOAS**: Implementação de maturidade nível 3 de Richardson (Navegabilidade por links).
* **Jakarta Validation**: Sanitização e validação rigorosa de dados via anotações (`@Valid`, `@Size`, `@Min`, `@Max`).
* **Springdoc OpenAPI (Swagger)**: Documentação interativa e customizada com OpenApiConfig.
* **H2 Database**: Banco de dados em memória para desenvolvimento rápido e testes.
* **RestTemplate**: Consumo de APIs externas (MangaDex).

---

Estrutura do Projeto e Arquitetura

O projeto segue uma arquitetura em camadas bem definida para garantir a separação de responsabilidades (SOLID):

```text
📦 senac.tsi.mangaVW
 ┣ 📂 controllers    # Exposição das rotas REST e montagem do HATEOAS
 ┣ 📂 entities       # Modelos de domínio e mapeamento JPA
 ┣ 📂 exceptions     # Tratamento de erros customizados e Global Advice
 ┣ 📂 infrastructure # Configurações (Swagger, LoadDatabase)
 ┣ 📂 repositories   # Interfaces do Spring Data JPA
 ┗ 📂 services       # Lógica de negócios complexa (Sincronização)
````

Modelo de Domínio (Relacionamentos)

O ecossistema de entidades foi projetado para evitar redundâncias e ciclos infinitos de serialização JSON (`@JsonIgnoreProperties`):

1.  **Manga**: Entidade central da aplicação.
2.  **Author (`1:N`)**: Um autor pode possuir múltiplas obras. Relacionamento obrigatório para criar um mangá.
3.  **Genre (`N:N`)**: Classificação taxonômica das obras (ex: Fantasia, Cyberpunk). A tabela ponte `manga_genre` é gerenciada automaticamente.
4.  **MangaDetails (`1:1`)**: Isolamento de informações técnicas (ISBN, Ano de Publicação, Status de Licenciamento).
5.  **Chapter (`1:N`)**: Estrutura episódica vinculada diretamente ao mangá. Permite rastreamento de idioma (`language`) e numeração.
6.  **Page (`1:N`)**: O conteúdo visual. Vinculada a um capítulo, armazena o número da página e a URL da imagem correspondente.

-----

 Principais Funcionalidades e Endpoints

A API é **Self-Descriptive** (HATEOAS). Todas as respostas `GET` incluem a seção `_links` permitindo que os clientes da API naveguem fluidamente entre as entidades associadas.

Mangás e Detalhes

* `GET /mangas`: Listagem completa com paginação.
* `GET /mangas/search?title={titulo}`: Busca textual de obras.
* `POST /mangas`: Criação de uma obra (permite vincular autores, gêneros e detalhes no mesmo JSON).
* `GET /manga-details/search?licensed=true`: Filtra obras pelo status de licenciamento oficial.

Autores e Gêneros

* `GET /authors/{id}`: Detalhes do autor com uma lista embutida de suas obras.
* `GET /genres/search?name={nome}`: Encontra categorias e retorna todos os mangás atrelados a ela.

Sistema de Leitura (Capítulos e Páginas)

* `GET /chapters/search?language={lang}`: Encontra capítulos em um idioma específico (ex: `pt-br`).
* `POST /chapters`: Adiciona um novo capítulo a um mangá existente (Requer ID do Mangá).
* `POST /pages`: Insere uma nova página de leitura.
* `GET /pages/search?imageUrl={url}`: Busca páginas pela URL do repositório de imagens.

Integração Externa (MangaDex)

* `POST /mangas/sync`: Rota aciona o `MangaDexService`, que consome a API oficial do MangaDex (`api.mangadex.org`). Ele importa automaticamente:
    * Título, Sinopse e Status.
    * Autor, Ano de Publicação e Gêneros.
    * Baixa a imagem da capa e converte automaticamente para o primeiro `Chapter` e a primeira `Page` no banco de dados.

-----

Validações e Tratamento de Erros

A API possui um **Global Exception Handler** (`MangaNotFoundAdvice`) que intercepta exceções em tempo de execução e garante respostas HTTP padronizadas.

* **404 Not Found**: Disparado por exceções customizadas (`MangaNotFoundException`, `ChapterNotFoundException`, etc.) quando um recurso requisitado ou relacionamento (ex: tentar criar um mangá com um autor inexistente) não é encontrado.
* **400 Bad Request**: Gerado pelo `@Valid` ao ferir regras de negócio (ex: ISBN fora do tamanho, Ano de publicação anterior a 1900 ou requisição com JSON malformado).

O Swagger foi customizado (`OpenApiConfig`) via `OperationCustomizer` para limpar códigos 404 de retornos onde logicamente não se aplicam (como `POST` para criar recursos raízes).

-----

Como Executar o Projeto Localmente

1.  **Pré-requisitos**: Certifique-se de ter o **JDK 17** e o **Maven** instalados em sua máquina.
2.  **Clone o repositório**:
    ```bash
    git clone [https://github.com/GleissonPDias/MangaVW.git](https://github.com/GleissonPDias/MangaVW.git)
    cd MangaVW
    ```
3.  **Execute a aplicação**:
    ```bash
    mvn spring-boot:run
    ```
4.  A aplicação inicializará o banco em memória e executará a classe `LoadDatabase`, populando o sistema com dados iniciais de teste (ex: Berserk e Bleach, com seus respectivos capítulos e páginas de exemplo).

Links Úteis

API em Produção (Render): [https://mangavw.onrender.com/swagger-ui/index.html](https://mangavw.onrender.com/swagger-ui/index.html)
* **Swagger UI (Local)**: [http://localhost:8080/swagger-ui.html](https://www.google.com/search?q=http://localhost:8080/swagger-ui.html)
* **H2 Console (Banco Local)**: [http://localhost:8080/h2-console](https://www.google.com/search?q=http://localhost:8080/h2-console)
  *(JDBC URL: `jdbc:h2:mem:mangadb`)*

-----

**Autor:** Gleisson - [GitHub](https://github.com/GleissonPDias)

