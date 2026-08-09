import http from 'k6/http';
import { check } from 'k6';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const SCENARIO = __ENV.SCENARIO || 'posts-list';
const JWT_SECRET = __ENV.JWT_SECRET || '';
const USER_PASSWORD = __ENV.LOAD_TEST_USER_PASSWORD || 'Loadtest1!';
const RPS = Number(__ENV.RPS || 1);
const DURATION = __ENV.DURATION || '45s';
const COMPARISON_ID = __ENV.COMPARISON_ID || 'unclassified';
const TEST_VARIANT = __ENV.TEST_VARIANT || 'unknown';
const GIT_BRANCH = __ENV.GIT_BRANCH || 'unknown';
const GIT_COMMIT = __ENV.GIT_COMMIT || 'unknown';
const CACHE_MODE = __ENV.CACHE_MODE || 'unknown';

const BASE_TAGS = {
  comparison_id: COMPARISON_ID,
  variant: TEST_VARIANT,
  branch: GIT_BRANCH,
  commit: GIT_COMMIT,
  cache_mode: CACHE_MODE,
};

const PUBLIC_SCENARIOS = new Set([
  'posts-list',
  'post-detail',
  'post-detail-popular',
  'popular-posts',
  'comment-list',
  'comment-list-popular',
]);

const SUPPORTED_SCENARIOS = new Set([
  'login',
  ...PUBLIC_SCENARIOS,
  'post-create',
  'comment-create',
  'post-like',
]);

const CACHE_METRICS = {
  'popular-posts': {
    duration: new Trend('cache_popular_list_duration', true),
    failed: new Rate('cache_popular_list_failed'),
    requests: new Counter('cache_popular_list_requests'),
  },
  'post-detail-popular': {
    duration: new Trend('cache_popular_detail_duration', true),
    failed: new Rate('cache_popular_detail_failed'),
    requests: new Counter('cache_popular_detail_requests'),
  },
  'comment-list-popular': {
    duration: new Trend('cache_popular_comments_duration', true),
    failed: new Rate('cache_popular_comments_failed'),
    requests: new Counter('cache_popular_comments_requests'),
  },
};

if (!SUPPORTED_SCENARIOS.has(SCENARIO)) {
  throw new Error(`Unsupported SCENARIO: ${SCENARIO}`);
}

if (!PUBLIC_SCENARIOS.has(SCENARIO) && SCENARIO !== 'login' && !JWT_SECRET) {
  throw new Error('JWT_SECRET is required for authenticated scenarios.');
}

export const options = {
  tags: {
    testid: `${COMPARISON_ID}-${TEST_VARIANT}`,
    ...BASE_TAGS,
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    endpoint: {
      executor: 'constant-arrival-rate',
      exec: 'runEndpoint',
      rate: RPS,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Math.min(Math.max(RPS, 10), 100),
      maxVUs: 200,
      gracefulStop: '5s',
    },
  },
  thresholds: {
    'checks{kind:target}': ['rate>0.99'],
    'http_req_failed{kind:target}': ['rate<0.01'],
  },
};

function paddedUserId(userId) {
  return String(userId).padStart(7, '0');
}

function currentUserId() {
  return ((__VU - 1) % 1000) + 1;
}

function currentPostId() {
  return ((__VU * 97 + __ITER) % 10000) + 1;
}

function currentPopularPostId() {
  return ((__VU + __ITER - 1) % 10) + 1;
}

function accessToken(userId) {
  const now = Math.floor(Date.now() / 1000);
  const header = encoding.b64encode(JSON.stringify({ alg: 'HS256' }), 'rawurl');
  const payload = encoding.b64encode(JSON.stringify({
    sub: String(userId),
    profileID: userId,
    role: 'USER',
    type: 'access',
    iat: now,
    exp: now + 3600,
  }), 'rawurl');
  const unsignedToken = `${header}.${payload}`;
  const signature = crypto.hmac('sha256', JWT_SECRET, unsignedToken, 'base64rawurl');
  return `${unsignedToken}.${signature}`;
}

function targetParams(userId, extraHeaders = {}) {
  return {
    headers: {
      Authorization: `Bearer ${accessToken(userId)}`,
      ...extraHeaders,
    },
    tags: { ...BASE_TAGS, kind: 'target', scenario: SCENARIO },
  };
}

function publicParams() {
  return { tags: { ...BASE_TAGS, kind: 'target', scenario: SCENARIO } };
}

function verify(response, expectedStatus) {
  check(response, {
    [`${SCENARIO} returns ${expectedStatus}`]: (res) => res.status === expectedStatus,
  }, { kind: 'target', scenario: SCENARIO });
}

function recordCacheMetrics(response, expectedStatus) {
  const metrics = CACHE_METRICS[SCENARIO];
  if (!metrics) {
    return;
  }

  const tags = { ...BASE_TAGS, cache_path: SCENARIO };
  metrics.duration.add(response.timings.duration, tags);
  metrics.failed.add(response.status !== expectedStatus, tags);
  metrics.requests.add(1, tags);
}

export function runEndpoint() {
  const userId = currentUserId();
  const postId = currentPostId();
  const popularPostId = currentPopularPostId();
  let response;
  let expectedStatus = 200;

  switch (SCENARIO) {
    case 'login':
      response = http.post(
        `${BASE_URL}/users/state`,
        JSON.stringify({
          email: `lt${paddedUserId(userId)}@load.test`,
          password: USER_PASSWORD,
        }),
        {
          headers: { 'Content-Type': 'application/json' },
          tags: { ...BASE_TAGS, kind: 'target', scenario: SCENARIO },
        },
      );
      break;

    case 'posts-list':
      response = http.get(`${BASE_URL}/posts?page=0&size=10&sort=latest`, publicParams());
      break;

    case 'post-detail':
      response = http.get(`${BASE_URL}/posts/${postId}`, publicParams());
      break;

    case 'post-detail-popular':
      response = http.get(`${BASE_URL}/posts/${popularPostId}`, publicParams());
      break;

    case 'popular-posts':
      response = http.get(`${BASE_URL}/posts/popular`, publicParams());
      break;

    case 'comment-list':
      response = http.get(`${BASE_URL}/comments/list/${postId}?page=0&size=10`, publicParams());
      break;

    case 'comment-list-popular':
      response = http.get(`${BASE_URL}/comments/list/${popularPostId}?page=0&size=10`, publicParams());
      break;

    case 'post-create':
      expectedStatus = 201;
      response = http.post(
        `${BASE_URL}/posts`,
        {
          title: `lt-${userId}-${__ITER % 100000}`,
          content: `Load-test post from VU ${__VU}, iteration ${__ITER}`,
        },
        targetParams(userId),
      );
      break;

    case 'comment-create':
      expectedStatus = 201;
      response = http.post(
        `${BASE_URL}/comments/post/${postId}`,
        JSON.stringify({ content: `Load-test comment ${__VU}-${__ITER}` }),
        targetParams(userId, { 'Content-Type': 'application/json' }),
      );
      break;

    case 'post-like':
      response = http.post(`${BASE_URL}/posts/${postId}/like`, null, targetParams(userId));
      break;

    default:
      throw new Error(`Unsupported SCENARIO: ${SCENARIO}`);
  }

  verify(response, expectedStatus);
  recordCacheMetrics(response, expectedStatus);
}

export function handleSummary(data) {
  if (!__ENV.SUMMARY_EXPORT_PATH) {
    return {};
  }

  return {
    [__ENV.SUMMARY_EXPORT_PATH]: JSON.stringify(data, null, 2),
  };
}
