#!/usr/bin/env node
const { spawnSync } = require('child_process');
const locale = (process.env.ISSUES_UI_LOCALE || 'pt').toLowerCase();
const configuration = locale === 'en' ? 'development-en' : 'development';
const result = spawnSync('npx', ['ng', 'serve', '--hmr', `--configuration=${configuration}`], {
  stdio: 'inherit',
  shell: true,
});
process.exit(result.status ?? 1);
