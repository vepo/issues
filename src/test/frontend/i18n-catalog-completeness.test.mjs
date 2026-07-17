import assert from 'node:assert/strict';
import { readdir, readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const webui = fileURLToPath(new URL('../../main/webui/', import.meta.url));
const applicationSource = path.join(webui, 'src', 'app');
const catalogs = {
  en: path.join(webui, 'public', 'i18n', 'en.json'),
  pt: path.join(webui, 'public', 'i18n', 'pt.json'),
};

async function filesBelow(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const nestedFiles = await Promise.all(entries.map(async entry => {
    const entryPath = path.join(directory, entry.name);
    if (entry.isDirectory() && ['.angular', 'dist', 'node_modules'].includes(entry.name)) {
      return [];
    }
    return entry.isDirectory() ? filesBelow(entryPath) : [entryPath];
  }));
  return nestedFiles.flat();
}

function isApplicationSource(file) {
  return /\.(?:html|ts)$/.test(file)
    && !file.endsWith('.spec.ts')
    && !file.includes(`${path.sep}generated${path.sep}`)
    && !file.includes(`${path.sep}testing${path.sep}`);
}

function flattenCatalog(value, prefix = '', flattened = new Map()) {
  for (const [name, child] of Object.entries(value)) {
    const key = prefix ? `${prefix}.${name}` : name;
    if (typeof child === 'string') {
      flattened.set(key, child);
    } else if (child && typeof child === 'object' && !Array.isArray(child)) {
      flattenCatalog(child, key, flattened);
    } else {
      flattened.set(key, child);
    }
  }
  return flattened;
}

function placeholders(message) {
  if (typeof message !== 'string') {
    return [];
  }
  return [...message.matchAll(/\{\{\s*([\w.]+)\s*\}\}/g)]
    .map(match => match[1])
    .sort();
}

function lineNumber(source, offset) {
  return source.slice(0, offset).split(/\r?\n/).length;
}

function relative(file) {
  return path.relative(webui, file).split(path.sep).join('/');
}

function recordReference(referencedKeys, key, file, source, offset) {
  const locations = referencedKeys.get(key) ?? [];
  locations.push(`${relative(file)}:${lineNumber(source, offset)}`);
  referencedKeys.set(key, locations);
}

function collectStaticTranslationKeys(source, file, referencedKeys) {
  const calls = /\b(?:translate|selectTranslate)\(\s*(['"])([a-z][\w-]*(?:\.[\w-]+)+)\1/g;
  for (const match of source.matchAll(calls)) {
    recordReference(referencedKeys, match[2], file, source, match.index);
  }

  const pipeExpressions = [
    /\{\{((?:(?!\{\{|\}\})[\s\S])*?)\|\s*transloco\b/g,
    /=\s*"([^"]*\|\s*transloco\b[^"]*)"/g,
    /=\s*'([^']*\|\s*transloco\b[^']*)'/g,
  ];
  for (const pattern of pipeExpressions) {
    for (const match of source.matchAll(pattern)) {
      const expressionOffset = match.index + match[0].indexOf(match[1]);
      const keyPattern = /(['"])([a-z][\w-]*(?:\.[\w-]+)+)\1/g;
      for (const keyMatch of match[1].matchAll(keyPattern)) {
        recordReference(
          referencedKeys,
          keyMatch[2],
          file,
          source,
          expressionOffset + keyMatch.index,
        );
      }
    }
  }

  const directives = /\b(?:transloco|\[transloco\])\s*=\s*["'](?:['"])?([a-z][\w-]*(?:\.[\w-]+)+)/g;
  for (const match of source.matchAll(directives)) {
    recordReference(referencedKeys, match[1], file, source, match.index);
  }
}

test('should keep runtime translation catalogs complete and remove compile-time i18n', async () => {
  const [enJson, ptJson, allSourceFiles, allWebuiFiles, angularJson] = await Promise.all([
    readFile(catalogs.en, 'utf8'),
    readFile(catalogs.pt, 'utf8'),
    filesBelow(applicationSource),
    filesBelow(webui),
    readFile(path.join(webui, 'angular.json'), 'utf8'),
  ]);
  const en = flattenCatalog(JSON.parse(enJson));
  const pt = flattenCatalog(JSON.parse(ptJson));
  const failures = [];

  const enOnly = [...en.keys()].filter(key => !pt.has(key)).sort();
  const ptOnly = [...pt.keys()].filter(key => !en.has(key)).sort();
  if (enOnly.length) failures.push(`Keys only in EN: ${enOnly.join(', ')}`);
  if (ptOnly.length) failures.push(`Keys only in PT: ${ptOnly.join(', ')}`);

  for (const key of [...en.keys()].filter(candidate => pt.has(candidate)).sort()) {
    const enPlaceholders = placeholders(en.get(key));
    const ptPlaceholders = placeholders(pt.get(key));
    if (enPlaceholders.join('\0') !== ptPlaceholders.join('\0')) {
      failures.push(
        `Placeholder mismatch for ${key}: EN [${enPlaceholders.join(', ')}], PT [${ptPlaceholders.join(', ')}]`,
      );
    }
  }

  const referencedKeys = new Map();
  const stalePatterns = [
    ['Angular i18n attribute', /\bi18n(?:-[\w-]+)?(?=\s|=|\/?>)/g],
    ['$localize call', /\$localize(?:\s*`|\s*\()/g],
    ['path-locale helper', /\b(?:localizedPath|localePath|pathForLocale|withLocalePrefix|stripLocalePrefix|localePrefix)\b/g],
  ];
  for (const file of allSourceFiles.filter(isApplicationSource)) {
    const source = await readFile(file, 'utf8');
    collectStaticTranslationKeys(source, file, referencedKeys);
    for (const [description, pattern] of stalePatterns) {
      for (const match of source.matchAll(pattern)) {
        failures.push(`${description}: ${relative(file)}:${lineNumber(source, match.index)}`);
      }
    }
  }

  for (const [key, locations] of [...referencedKeys].sort(([left], [right]) => left.localeCompare(right))) {
    if (!en.has(key) || !pt.has(key)) {
      failures.push(
        `Missing referenced key ${key} in ${[
          !en.has(key) && 'EN',
          !pt.has(key) && 'PT',
        ].filter(Boolean).join(' and ')} (${locations.join(', ')})`,
      );
    }
  }

  const xlfAssets = allWebuiFiles
    .filter(file => /\.(?:xlf|xliff)$/i.test(file))
    .map(relative)
    .sort();
  if (xlfAssets.length) failures.push(`XLF assets remain: ${xlfAssets.join(', ')}`);

  const angular = JSON.parse(angularJson);
  const project = angular.projects.issues;
  if (project.i18n !== undefined) failures.push('Locale-prefixed build config: projects.issues.i18n');
  for (const targetName of ['build', 'serve']) {
    const target = project.architect[targetName];
    for (const [configurationName, configuration] of Object.entries(target.configurations ?? {})) {
      if (/(?:^|[-_])(?:en|pt)(?:$|[-_])/.test(configurationName)) {
        failures.push(`Locale-prefixed build config: ${targetName}.configurations.${configurationName}`);
      }
      if (configuration.localize !== undefined || configuration.baseHref !== undefined) {
        failures.push(`Locale-prefixed build config: ${targetName}.configurations.${configurationName}`);
      }
    }
    if (target.options?.localize !== undefined || target.options?.baseHref !== undefined) {
      failures.push(`Locale-prefixed build config: ${targetName}.options`);
    }
  }

  if (failures.length) {
    assert.fail(`i18n completeness failures:\n- ${failures.join('\n- ')}`);
  }
});
