const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const now = new Date();

const BRANCH = executeGitCommand('git rev-parse --abbrev-ref HEAD');

function getCorrectVersion(lib) {
  let branch;
  switch (BRANCH) {
    case 'main': {
      branch = executeGitCommand(`npm view ${lib} version`);
      break;
    }

    case 'develop': {
      branch = 'develop';
      break;
    }

    case 'develop-pedago': {
      branch = 'develop-pedago';
      break;
    }

    case 'develop-b2school': {
      branch = 'develop-b2school';
      break;
    }

    default: {
      branch = 'develop';
      break;
    }
  }

  return branch;
}

function executeGitCommand(command) {
  return execSync(command)
    .toString('utf8')
    .replace(/[\n\r\s]+$/, '');
}

function generateVersion() {
  let year = now.getFullYear();
  let month = now.getMonth();
  let days = now.getDate();
  let hours = now.getHours();
  let minutes = now.getMinutes();
  let format = '';

  month = month + 1;
  if (month < 10) month = `0${month}`;
  if (minutes < 10) minutes = `0${minutes}`;

  format = `${year}${month}${days}${hours}${minutes}`;

  return format;
}

function generatePackage(content) {
  fs.writeFile(
    path.resolve(__dirname, '../package.json'),
    JSON.stringify(content, null, 2),
    (err) => {
      if (err) {
        console.error(err);
      }
      console.log(`version generated: ${content.version}`);
    },
  );
}

function generateDeps(content) {
  const deps = { ...content.dependencies };

  // Find all @edifice.io dependencies and update their versions
  Object.keys(deps).forEach((dep) => {
    if (dep.startsWith('@edifice.io/')) {
      deps[dep] = getCorrectVersion(dep);
    }
  });

  return deps;
}

function createPackage() {
  fs.readFile(
    path.resolve(__dirname, '../package.json.template'),
    (err, data) => {
      if (err) {
        console.error(err);
        return;
      }

      let content = JSON.parse(data);
      let version = content.version;

      version = version.replace('%branch%', BRANCH);
      version = version.replace('%generateVersion%', generateVersion());

      content.version = version;
      content.dependencies = generateDeps(content);

      generatePackage(content);
    },
  );
}

createPackage();
