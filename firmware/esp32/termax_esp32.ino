#include <DHT.h>

#if defined(ESP8266)
  #include <ESP8266WiFi.h>
  #include <ESP8266HTTPClient.h>
#else
  #include <WiFi.h>
  #include <HTTPClient.h>
#endif

#include <secrets.h>

// ---------- Modo de conexao ----------
// Descomente para HTTPS (tunel). Comentado = HTTP na rede local.
 #define USAR_HTTPS

#ifdef USAR_HTTPS
  #include <WiFiClientSecure.h>
  const char* SERVER_URL = API_URL;
#else
  // IP do PC que roda a API (rode ipconfig). Nao usar localhost.
  const char* SERVER_URL = API_URL2;
#endif

// ---------- Configuracao ----------
#define DHT_PIN   10
#define DHT_TYPE  DHT22

const char* DEVICE_ID = "esp32-dht22";

const unsigned long INTERVALO_ENVIO   = 10000;  // 10 s
const unsigned long TIMEOUT_WIFI_INIT = 20000;
const unsigned long TIMEOUT_HTTP      = 8000;

DHT dht(DHT_PIN, DHT_TYPE);

unsigned long ultimoEnvio = 0;
bool primeiraExecucao = true;
bool dadosExibidos = false;

// ---------- Wi-Fi ----------
void conectarWiFi() {
  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);

  Serial.print("Conectando a: ");
  Serial.println(WIFI_SSID);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  unsigned long inicio = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - inicio < TIMEOUT_WIFI_INIT) {
    Serial.print(".");
    delay(500);
  }
  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("Conexao estabelecida!");
  } else {
    Serial.println("Nao foi possivel conectar. Tentando em segundo plano...");
  }
}

void exibirInfoRede() {
  Serial.println();
  Serial.println("===== INFORMACOES DA REDE =====");
  Serial.print("SSID: ");         Serial.println(WiFi.SSID());
  Serial.print("Endereco IP: ");  Serial.println(WiFi.localIP());
  Serial.print("Endereco MAC: "); Serial.println(WiFi.macAddress());
  Serial.print("Destino: ");      Serial.println(SERVER_URL);
  Serial.println("==============================");
}

// ---------- JSON ----------
void montarJson(char* buffer, size_t tamanho, float temp, float hum) {
  snprintf(buffer, tamanho,
           "{\"deviceId\":\"%s\",\"temperature\":%.1f,\"humidity\":%.1f}",
           DEVICE_ID, temp, hum);
}

// ---------- HTTP / HTTPS ----------
bool enviarDados(float temp, float hum) {
  char json[128];
  montarJson(json, sizeof(json), temp, hum);

#ifdef USAR_HTTPS
  WiFiClientSecure client;
  client.setInsecure();  // sem validar certificado (so para teste)
#else
  WiFiClient client;
#endif

  HTTPClient http;

  if (!http.begin(client, SERVER_URL)) {
    Serial.println("Erro ao iniciar HTTP");
    return false;
  }

  http.addHeader("Content-Type", "application/json");
#ifdef USAR_HTTPS
  http.addHeader("X-Tunnel-Skip-AntiPhishing-Page", "true");
#endif
  http.setTimeout(TIMEOUT_HTTP);

  int codigo = http.POST(json);

  if (codigo > 0) {
    Serial.printf("POST -> HTTP %d | %s\n", codigo, json);
    if (codigo != 201) {  // a API devolve 201 (Created) em caso de sucesso
      Serial.println("Resposta inesperada: " + http.getString());
    }
  } else {
    Serial.printf("Falha no envio: %s\n", http.errorToString(codigo).c_str());
  }

  http.end();
  return codigo == 201;
}

// ---------- Setup / Loop ----------
void setup() {
  Serial.begin(115200);
  dht.begin();
  delay(1000);

  Serial.println();
  Serial.println("==============================");
  Serial.println("  TERMOMETRO DE FREEZER (HTTP)");
  Serial.println("==============================");

  conectarWiFi();
}

void loop() {
  if (WiFi.status() == WL_CONNECTED) {

    if (!dadosExibidos) {
      exibirInfoRede();
      dadosExibidos = true;
    }

    if (primeiraExecucao || millis() - ultimoEnvio >= INTERVALO_ENVIO) {
      primeiraExecucao = false;
      ultimoEnvio = millis();

      float temp = dht.readTemperature();
      float hum  = dht.readHumidity();

      if (isnan(temp) || isnan(hum)) {
        Serial.println("Erro na leitura do DHT22!");
        return;  // tenta de novo no proximo ciclo
      }

      Serial.printf("Temperatura: %.1f C | Umidade: %.1f %%RH | RSSI: %d dBm\n",
                    temp, hum, WiFi.RSSI());

      enviarDados(temp, hum);
      Serial.println("------------------------------");
    }

  } else {

    if (dadosExibidos) {
      Serial.println();
      Serial.println("ATENCAO: CONEXAO PERDIDA!");
      dadosExibidos = false;
    }

    if (millis() - ultimoEnvio >= INTERVALO_ENVIO) {
      ultimoEnvio = millis();
      Serial.println("Estado: DESCONECTADO | Aguardando reconexao...");
    }
  }
}