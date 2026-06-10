import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 50  }, // arranque
    { duration: '1m',  target: 200 }, // carga alta
    { duration: '1m',  target: 500 }, // carga muy alta
    { duration: '1m',  target: 1000}, // límite — 1000 usuarios simultáneos
    { duration: '30s', target: 0   }, // bajada
  ],
  thresholds: {
    http_req_duration: ['p(95)<5000'], // subimos el umbral porque habrá más latencia
    http_req_failed:   ['rate<0.10'],  // permitimos hasta 10% de errores
  },
};

const BASE_URL = 'http://localhost:8080/api/v1';

// ─── Setup — login una sola vez, token compartido por todos los VUs ───
export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({
      identifier: 'k6test@taskflow.com',
      password:   'K6TestPass123!'
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(loginRes, {
    'setup: login exitoso': (r) => r.status === 200,
  });

  const body = JSON.parse(loginRes.body);
  return { token: body.tokens.accessToken };
}

// ─── Escenario principal ───────────────────────────────────────────────
export default function (data) {
  const headers = {
    'Content-Type':  'application/json',
    'Authorization': `Bearer ${data.token}`,
  };

  // 1. Listar notas
  const listRes = http.get(`${BASE_URL}/notes`, { headers });
  check(listRes, {
    'GET /notes → 200': (r) => r.status === 200,
  });

  sleep(0.5);

  // 2. Crear nota
  const createRes = http.post(
    `${BASE_URL}/notes`,
    JSON.stringify({
      title:   `Nota k6 ${Date.now()}`,
      content: 'Contenido generado por prueba de carga'
    }),
    { headers }
  );
  check(createRes, {
    'POST /notes → 201': (r) => r.status === 201,
  });

  sleep(0.5);

  // 3. Listar tags
  const tagsRes = http.get(`${BASE_URL}/tags`, { headers });
  check(tagsRes, {
    'GET /tags → 200': (r) => r.status === 200,
  });

  sleep(1);
}