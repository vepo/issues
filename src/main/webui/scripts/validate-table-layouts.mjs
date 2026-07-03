import { chromium } from 'playwright';
import { execSync } from 'node:child_process';

const baseUrl = process.env.BASE_URL ?? 'http://localhost:8080';
const email = process.env.VALIDATE_EMAIL ?? 'cto@issues.ui';
const password = process.env.VALIDATE_PASSWORD ?? 'qwas1234';

const loginJson = execSync(
  `curl -s -X POST ${baseUrl}/api/auth/login -H 'Content-Type: application/json' -d '${JSON.stringify({ email, password })}'`,
  { encoding: 'utf8' }
);
const token = JSON.parse(loginJson).token;
if (!token) {
  console.error('Login failed:', loginJson);
  process.exit(1);
}

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1400, height: 900 } });

await page.goto(baseUrl, { waitUntil: 'domcontentloaded' });
await page.evaluate((jwt) => localStorage.setItem('jwt_token', jwt), token);
await page.goto(`${baseUrl}/`, { waitUntil: 'domcontentloaded' });
await page.waitForSelector('.data-table--layout-grid.data-table--cols-home-tickets .header', { timeout: 15000 });

const homeGrid = await page.$eval(
  '.data-table--layout-grid.data-table--cols-home-tickets .header',
  (el) => getComputedStyle(el).display
);
const homeRowGrid = await page.$eval(
  '.data-table--layout-grid.data-table--cols-home-tickets .body .row',
  (el) => getComputedStyle(el).display
);

if (homeGrid !== 'grid' || homeRowGrid !== 'grid') {
  console.error('FAIL home table layout:', { homeGrid, homeRowGrid });
  process.exit(1);
}
console.log('OK home table uses grid:', { homeGrid, homeRowGrid });
await page.screenshot({ path: '/tmp/issues-home-table.png', fullPage: false });

await page.goto(`${baseUrl}/projects`, { waitUntil: 'domcontentloaded' });
await page.waitForSelector('.data-table--layout-table .cell-actions', { timeout: 15000 });
const projectsTable = await page.$eval(
  '.data-table--layout-table.data-table--cols-projects-list',
  (el) => getComputedStyle(el).display
);
const projectsActions = await page.$eval('.data-table--layout-table .cell-actions', (el) => {
  const style = getComputedStyle(el);
  return { display: style.display, whiteSpace: style.whiteSpace, width: style.width };
});

if (projectsTable !== 'table') {
  console.error('FAIL projects table layout:', projectsTable);
  process.exit(1);
}
console.log('OK projects table uses table display');
console.log('OK projects cell-actions:', projectsActions);

const actionButtons = await page.$$eval('.data-table--layout-table .cell-actions .btn', (nodes) =>
  nodes.map((n) => ({ text: n.textContent?.trim(), visible: n.offsetParent !== null }))
);
console.log('OK project action buttons:', actionButtons);

await page.screenshot({ path: '/tmp/issues-projects-table.png', fullPage: false });
await browser.close();
console.log('Screenshots: /tmp/issues-home-table.png, /tmp/issues-projects-table.png');
