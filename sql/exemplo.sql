DROP TABLE IF EXISTS teste_termax;

CREATE TABLE teste_termax (
    id SERIAL PRIMARY KEY,
    temperatura NUMERIC(4,1),
    umidade NUMERIC(4,1),
    criado_em TIMESTAMP DEFAULT NOW()
);

INSERT INTO teste_termax (temperatura, umidade) VALUES 
    (25.4, 58.0),
    (25.6, 57.2);

SELECT * FROM teste_termax;