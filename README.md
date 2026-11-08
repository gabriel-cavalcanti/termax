# TERMAX — Monitoramento de Temperatura e Umidade

> **Aviso:** o TERMAX é um protótipo educacional. Não é dispositivo médico, não diagnostica, não controla o ambiente e não está validado para garantir a conservação de medicamentos, amostras ou qualquer material. Limites e alertas são demonstrativos. Sem dados de pessoas.

## Arquitetura do MVP
`Sensor -> Microcontrolador -> Wi-Fi (HTTP/JSON) -> API Spring Boot -> PostgreSQL -> Dashboard`

* **Modelo A (Referência):** ESP32 + DHT22/AM2302
* **Modelo B (Experimental):** ESP8266 (NodeMCU) + AHT20

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
