#!/usr/bin/env node
/**
 * Merge extracted messages.xlf into messages.en.xlf.
 * Keeps existing English targets; new units get source as target (PT) until translated.
 */
const fs = require('fs');
const path = require('path');

const localeDir = path.join(__dirname, '..', 'src', 'locale');
const sourcePath = path.join(localeDir, 'messages.xlf');
const enPath = path.join(localeDir, 'messages.en.xlf');

const KNOWN = {
  'system.priority.critical': 'Critical',
  'system.priority.high': 'High',
  'system.priority.medium': 'Medium',
  'system.priority.low': 'Low',
  'system.phase.planned': 'Planned',
  'system.phase.active': 'Active',
  'system.phase.completed': 'Completed',
  'system.ticketType.epic': 'Epic',
  'system.ticketType.story': 'Story',
  'system.ticketType.task': 'Task',
  'system.link.blocks': 'Blocks',
  'system.link.relates': 'Related to',
  'system.link.duplicates': 'Duplicate of',
  'system.link.derived': 'Derived from',
  'system.link.remaining': 'Remaining work of',
  'login.invalidCredentials': 'Invalid email or password',
  'account.loadError': 'Could not load your profile.',
  'account.profileSaved': 'Profile updated successfully.',
  'account.profileSaveError': 'Could not update the profile. Check the email address.',
};

function parseTargets(xlf) {
  const map = new Map();
  const re = /<trans-unit[^>]*id="([^"]+)"[\s\S]*?<\/trans-unit>/g;
  let m;
  while ((m = re.exec(xlf))) {
    const id = m[1];
    const block = m[0];
    const target = block.match(/<target[^>]*>([\s\S]*?)<\/target>/);
    if (target) {
      map.set(id, target[1]);
    }
  }
  return map;
}

const source = fs.readFileSync(sourcePath, 'utf8');
const existingEn = fs.existsSync(enPath) ? fs.readFileSync(enPath, 'utf8') : '';
const existingTargets = parseTargets(existingEn);

const units = [];
const unitRe = /<trans-unit[\s\S]*?<\/trans-unit>/g;
let match;
while ((match = unitRe.exec(source))) {
  let unit = match[0];
  const idMatch = unit.match(/id="([^"]+)"/);
  const sourceMatch = unit.match(/<source>([\s\S]*?)<\/source>/);
  if (!idMatch || !sourceMatch) {
    continue;
  }
  const id = idMatch[1];
  const sourceText = sourceMatch[1];
  const targetText = KNOWN[id] ?? existingTargets.get(id) ?? sourceText;
  if (unit.includes('<target')) {
    unit = unit.replace(/<target[^>]*>[\s\S]*?<\/target>/, `<target>${targetText}</target>`);
  } else {
    unit = unit.replace('</source>', `</source>\n        <target>${targetText}</target>`);
  }
  units.push(unit);
}

const out = `<?xml version="1.0" encoding="UTF-8" ?>
<xliff version="1.2" xmlns="urn:oasis:names:tc:xliff:document:1.2">
  <file source-language="pt" datatype="plaintext" original="ng2.template" target-language="en">
    <body>
${units.map(u => '      ' + u.replace(/\n/g, '\n      ')).join('\n')}
    </body>
  </file>
</xliff>
`;
fs.writeFileSync(enPath, out);
console.log(`Wrote ${units.length} units to messages.en.xlf`);
