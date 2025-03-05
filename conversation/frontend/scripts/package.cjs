const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const now = new Date();

const BRANCH = executeGitCommand('git rev-parse --abbrev-ref HEAD');

function executeGitCommand(command) {
  try {
    return execSync(command)
      .toString('utf8')
      .replace(/[\n\r\s]+$/, '');
  } catch (error) {
    console.error(`Error executing command "${command}": ${error.message}`);
    process.exit(1);
  }
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
        process.exit(1);
      }
      console.log(`version generated: ${content.version}`);
    },
  );
}

function generateDeps(content) {
  return {
    ...content.dependencies,
    '@edifice-ui/icons': BRANCH,
    '@edifice-ui/react': BRANCH,
  };
}

function generateDevDeps(content) {
  return {
    ...content.devDependencies,
    'edifice-ts-client': BRANCH,
    'edifice-bootstrap': BRANCH,
  };
}

function createPackage() {
  fs.readFile(path.resolve(__dirname, '../package.json'), (err, data) => {
    if (err) {
      console.error(err);
      process.exit(1);
    }

    let content = JSON.parse(data);
    let version = content.version;

    version = version.replace('%branch%', BRANCH);
    version = version.replace('%generateVersion%', generateVersion());

    content.version = version;
    content.dependencies = generateDeps(content);
    content.devDependencies = generateDevDeps(content);

    generatePackage(content);
  });
}

createPackage();
