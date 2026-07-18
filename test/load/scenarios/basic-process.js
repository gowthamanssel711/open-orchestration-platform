import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.GATEWAY_URL || 'http://localhost:8080';
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8180';

const errorRate = new Rate('errors');
const startProcessDuration = new Trend('start_process_duration');
const queryDuration = new Trend('query_duration');

export const options = {
  stages: [
    { duration: '30s', target: 5 },   // Ramp up to 5 users
    { duration: '1m', target: 10 },   // Hold at 10 users
    { duration: '30s', target: 20 },  // Spike to 20
    { duration: '30s', target: 0 },   // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    errors: ['rate<0.1'],
    start_process_duration: ['p(95)<2000'],
    query_duration: ['p(95)<500'],
  },
};

let token = '';

export function setup() {
  const tokenRes = http.post(
    `${KEYCLOAK_URL}/realms/oop/protocol/openid-connect/token`,
    {
      grant_type: 'password',
      client_id: 'admin-cli',
      username: 'admin',
      password: 'admin',
    },
    { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
  );

  check(tokenRes, { 'got token': (r) => r.status === 200 });
  return { token: JSON.parse(tokenRes.body).access_token };
}

export default function (data) {
  const headers = {
    Authorization: `Bearer ${data.token}`,
    'Content-Type': 'application/json',
  };

  // Start a process instance
  const startRes = http.post(
    `${BASE_URL}/api/v1/process-instances`,
    JSON.stringify({
      processDefinitionKey: 'sampleOrderProcess',
      variables: {
        orderId: `ORD-${__VU}-${__ITER}`,
        amount: Math.random() * 1000,
      },
    }),
    { headers, tags: { name: 'start_process' } }
  );

  startProcessDuration.add(startRes.timings.duration);
  const startOk = check(startRes, {
    'process started': (r) => r.status === 201 || r.status === 200,
  });
  errorRate.add(!startOk);

  sleep(1);

  // Query process instances
  const queryRes = http.get(
    `${BASE_URL}/api/v1/query/process-instances?size=10`,
    { headers, tags: { name: 'query_instances' } }
  );

  queryDuration.add(queryRes.timings.duration);
  const queryOk = check(queryRes, {
    'query succeeded': (r) => r.status === 200,
  });
  errorRate.add(!queryOk);

  sleep(1);

  // List tasks
  const tasksRes = http.get(
    `${BASE_URL}/api/v1/tasks`,
    { headers, tags: { name: 'list_tasks' } }
  );

  check(tasksRes, { 'tasks listed': (r) => r.status === 200 });

  sleep(0.5);
}

export function handleSummary(data) {
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
    'test/load/results/summary.json': JSON.stringify(data),
  };
}

function textSummary(data, opts) {
  // k6 built-in handles this; fallback for clarity
  return JSON.stringify(data.metrics, null, 2);
}
