"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.GitRepository = void 0;
exports.cloneFromUpstream = cloneFromUpstream;
exports.updateRebaseFile = updateRebaseFile;
exports.fetchGitRemote = fetchGitRemote;
exports.getGithubSlugOrNull = getGithubSlugOrNull;
exports.extractUserAndRepoFromGitHubUrl = extractUserAndRepoFromGitHubUrl;
exports.commitChanges = commitChanges;
exports.getLatestCommitSha = getLatestCommitSha;
const child_process_1 = require("child_process");
const devkit_exports_1 = require("../devkit-exports");
const path_1 = require("path");
const SQUASH_EDITOR = (0, path_1.join)(__dirname, 'squash.js');
function execAsync(command, execOptions) {
    return new Promise((res, rej) => {
        (0, child_process_1.exec)(command, execOptions, (err, stdout, stderr) => {
            if (err) {
                return rej(err);
            }
            res(stdout);
        });
    });
}
async function cloneFromUpstream(url, destination, { originName } = { originName: 'origin' }) {
    await execAsync(`git clone ${url} ${destination} --depth 1 --origin ${originName}`, {
        cwd: (0, path_1.dirname)(destination),
    });
    return new GitRepository(destination);
}
class GitRepository {
    constructor(directory) {
        this.directory = directory;
        this.root = this.getGitRootPath(this.directory);
    }
    getGitRootPath(cwd) {
        return (0, child_process_1.execSync)('git rev-parse --show-toplevel', {
            cwd,
        })
            .toString()
            .trim();
    }
    addFetchRemote(remoteName, branch) {
        return this.execAsync(`git config --add remote.${remoteName}.fetch "+refs/heads/${branch}:refs/remotes/${remoteName}/${branch}"`);
    }
    execAsync(command) {
        return execAsync(command, {
            cwd: this.root,
        });
    }
    async showStat() {
        return await this.execAsync(`git show --stat`);
    }
    async listBranches() {
        return (await this.execAsync(`git ls-remote --heads --quiet`))
            .trim()
            .split('\n')
            .map((s) => s
            .trim()
            .substring(s.indexOf('\t') + 1)
            .replace('refs/heads/', ''));
    }
    async getGitFiles(path) {
        return (await this.execAsync(`git ls-files ${path}`))
            .trim()
            .split('\n')
            .map((s) => s.trim())
            .filter(Boolean);
    }
    async reset(ref) {
        return this.execAsync(`git reset ${ref} --hard`);
    }
    async squashLastTwoCommits() {
        return this.execAsync(`git -c core.editor="node ${SQUASH_EDITOR}" rebase --interactive --no-autosquash HEAD~2`);
    }
    async mergeUnrelatedHistories(ref, message) {
        return this.execAsync(`git merge ${ref} -X ours --allow-unrelated-histories -m "${message}"`);
    }
    async fetch(remote, ref) {
        return this.execAsync(`git fetch ${remote}${ref ? ` ${ref}` : ''}`);
    }
    async checkout(branch, opts) {
        return this.execAsync(`git checkout ${opts.new ? '-b ' : ' '}${branch}${opts.base ? ' ' + opts.base : ''}`);
    }
    async move(path, destination) {
        return this.execAsync(`git mv ${path} ${destination}`);
    }
    async push(ref, remoteName) {
        return this.execAsync(`git push -u -f ${remoteName} ${ref}`);
    }
    async commit(message) {
        return this.execAsync(`git commit -am "${message}"`);
    }
    async amendCommit() {
        return this.execAsync(`git commit --amend -a --no-edit`);
    }
    deleteGitRemote(name) {
        return this.execAsync(`git remote rm ${name}`);
    }
    deleteBranch(branch) {
        return this.execAsync(`git branch -D ${branch}`);
    }
    addGitRemote(name, url) {
        return this.execAsync(`git remote add ${name} ${url}`);
    }
}
exports.GitRepository = GitRepository;
/**
 * This is used by the squash editor script to update the rebase file.
 */
function updateRebaseFile(contents) {
    const lines = contents.split('\n');
    const lastCommitIndex = lines.findIndex((line) => line === '') - 1;
    lines[lastCommitIndex] = lines[lastCommitIndex].replace('pick', 'fixup');
    return lines.join('\n');
}
function fetchGitRemote(name, branch, execOptions) {
    return (0, child_process_1.execSync)(`git fetch ${name} ${branch} --depth 1`, execOptions);
}
function getGithubSlugOrNull() {
    try {
        const gitRemote = (0, child_process_1.execSync)('git remote -v', {
            stdio: 'pipe',
        }).toString();
        // If there are no remotes, we default to github
        if (!gitRemote || gitRemote.length === 0) {
            return 'github';
        }
        return extractUserAndRepoFromGitHubUrl(gitRemote);
    }
    catch (e) {
        // Probably git is not set up, so we default to github
        return 'github';
    }
}
function extractUserAndRepoFromGitHubUrl(gitRemotes) {
    const regex = /^\s*(\w+)\s+(git@github\.com:|https:\/\/github\.com\/)([A-Za-z0-9_.-]+)\/([A-Za-z0-9_.-]+)\.git/gm;
    const remotesPriority = ['origin', 'upstream', 'base'];
    const foundRemotes = {};
    let firstGitHubUrl = null;
    let match;
    while ((match = regex.exec(gitRemotes)) !== null) {
        const remoteName = match[1];
        const url = match[2] + match[3] + '/' + match[4] + '.git';
        foundRemotes[remoteName] = url;
        if (!firstGitHubUrl) {
            firstGitHubUrl = url;
        }
    }
    for (const remote of remotesPriority) {
        if (foundRemotes[remote]) {
            return parseGitHubUrl(foundRemotes[remote]);
        }
    }
    return firstGitHubUrl ? parseGitHubUrl(firstGitHubUrl) : null;
}
function parseGitHubUrl(url) {
    const sshPattern = /git@github\.com:([A-Za-z0-9_.-]+)\/([A-Za-z0-9_.-]+)\.git/;
    const httpsPattern = /https:\/\/github\.com\/([A-Za-z0-9_.-]+)\/([A-Za-z0-9_.-]+)\.git/;
    let match = url.match(sshPattern) || url.match(httpsPattern);
    if (match) {
        return `${match[1]}/${match[2]}`;
    }
    return null;
}
function commitChanges(commitMessage, directory) {
    try {
        (0, child_process_1.execSync)('git add -A', { encoding: 'utf8', stdio: 'pipe' });
        (0, child_process_1.execSync)('git commit --no-verify -F -', {
            encoding: 'utf8',
            stdio: 'pipe',
            input: commitMessage,
            cwd: directory,
        });
    }
    catch (err) {
        if (directory) {
            // We don't want to throw during create-nx-workspace
            // because maybe there was an error when setting up git
            // initially.
            devkit_exports_1.logger.verbose(`Git may not be set up correctly for this new workspace.
        ${err}`);
        }
        else {
            throw new Error(`Error committing changes:\n${err.stderr}`);
        }
    }
    return getLatestCommitSha();
}
function getLatestCommitSha() {
    try {
        return (0, child_process_1.execSync)('git rev-parse HEAD', {
            encoding: 'utf8',
            stdio: 'pipe',
        }).trim();
    }
    catch {
        return null;
    }
}
