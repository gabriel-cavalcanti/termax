# TERMAX — Monitoramento de Temperatura e Umidade

> **Aviso:** o TERMAX é um protótipo educacional. Não é dispositivo médico, não diagnostica, não controla o ambiente e não está validado para garantir a conservação de medicamentos, amostras ou qualquer material. Limites e alertas são demonstrativos. Sem dados de pessoas.

## Arquitetura do MVP
`Sensor -> Microcontrolador -> Wi-Fi (HTTP/JSON) -> API Spring Boot -> PostgreSQL -> Dashboard`

* **Modelo A (Referência):** ESP32 + DHT22/AM2302
* **Modelo B (Experimental):** ESP8266 (NodeMCU) + AHT20

## Stack Tecnológica
* **Java 21 (LTS)** — Linguagem backend com `records` imutáveis
* **Spring Boot 3.4** — API REST e integração JDBC (`JdbcTemplate`)
* **PostgreSQL 16 (Alpine)** — Banco de dados relacional containerizado com healthcheck
* **Docker & Docker Compose** — Orquestração local de infraestrutura
* **HTML5, CSS3 & Chart.js** — Dashboard SaaS/IoT em tema escuro com telemetria em tempo real

## Estrutura do Repositório
* `/firmware/esp32` — Código C/C++ para o Modelo A
* `/firmware/esp8266` — Código C/C++ para o Modelo B
* `/backend` — API REST em Java (Spring Boot)
* `/frontend` — Dashboard Web (HTML, CSS, JS + Chart.js)
* `/docs` — Contratos, esquemas de hardware, decisões e pesquisas

## Como subir o Banco de Dados (Local)
1. Copie o arquivo de exemplo de variáveis de ambiente: `cp .env.example .env`
2. Suba o container do PostgreSQL: `docker compose up -d`
3. Para desligar o banco: `docker compose down`

## Como executar o Backend de Validação
1. Certifique-se de que o container do PostgreSQL está em execução (`docker compose ps`).
2. Na pasta `/backend`, inicie a API Spring Boot:
   ```bash
   ./mvnw spring-boot:run
   ```
   *(No Windows PowerShell/CMD, utilize `.\mvnw.cmd spring-boot:run`)*
3. Acesse o endpoint de telemetria em `http://localhost:8080/api/v1/teste` (ou `/api/v1/readings`) e abra `frontend/index.html` no navegador.
