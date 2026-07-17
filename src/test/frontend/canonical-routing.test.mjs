import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const webui = new URL('../../main/webui/', import.meta.url);
const repositoryRoot = new URL('../../../', import.meta.url);

test('should build one locale-neutral Angular application', async () => {
  const angular = JSON.parse(await readFile(new URL('angular.json', webui), 'utf8'));
  const project = angular.projects.issues;
  const buildConfigurations = project.architect.build.configurations;
  const serveConfigurations = project.architect.serve.configurations;

  assert.equal(project.i18n?.sourceLocale?.baseHref, undefined);
  assert.equal(project.i18n?.locales?.en?.baseHref, undefined);
  assert.notEqual(buildConfigurations.production.localize, true);
  assert.equal(buildConfigurations.development.localize, undefined);
  assert.equal(buildConfigurations['development-en'], undefined);
  assert.equal(serveConfigurations['development-en'], undefined);
});

test('should use the canonical Angular dev command without locale scripts', async () => {
  const packageJson = JSON.parse(await readFile(new URL('package.json', webui), 'utf8'));
  const applicationProperties = await readFile(
    new URL('src/main/resources/application.properties', repositoryRoot),
    'utf8',
  );

  assert.equal(packageJson.scripts['start:locale'], undefined);
  assert.equal(packageJson.scripts['start:pt'], undefined);
  assert.equal(packageJson.scripts['start:en'], undefined);
  assert.match(applicationProperties, /quarkus\.quinoa\.dev-server\.command=npm run start(?:\r?\n|$)/);
  assert.doesNotMatch(applicationProperties, /start:locale/);
});
