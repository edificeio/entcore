"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.importHandler = importHandler;
const path_1 = require("path");
const git_utils_1 = require("../../utils/git-utils");
const promises_1 = require("node:fs/promises");
const tmp_1 = require("tmp");
const enquirer_1 = require("enquirer");
const output_1 = require("../../utils/output");
const createSpinner = require("ora");
const init_v2_1 = require("../init/init-v2");
const nx_json_1 = require("../../config/nx-json");
const workspace_root_1 = require("../../utils/workspace-root");
const package_manager_1 = require("../../utils/package-manager");
const workspace_context_1 = require("../../utils/workspace-context");
const utils_1 = require("../init/implementation/utils");
const command_line_utils_1 = require("../../utils/command-line-utils");
const prepare_source_repo_1 = require("./utils/prepare-source-repo");
const merge_remote_source_1 = require("./utils/merge-remote-source");
const needs_install_1 = require("./utils/needs-install");
const importRemoteName = '__tmp_nx_import__';
async function importHandler(options) {
    let { sourceRemoteUrl, ref, source, destination } = options;
    output_1.output.log({
        title: 'Nx will walk you through the process of importing code from another repository into this workspace:',
        bodyLines: [
            `1. Nx will clone the other repository into a temporary directory`,
            `2. Code to be imported will be moved to the same directory it will be imported into on a temporary branch`,
            `3. The code will be merged into the current branch in this workspace`,
            `4. Nx will recommend plugins to integrate tools used in the imported code with Nx`,
            `5. The code will be successfully imported into this workspace`,
            '',
            `Git history will be preserved during this process`,
        ],
    });
    const tempImportDirectory = (0, path_1.join)(tmp_1.tmpdir, 'nx-import');
    if (!sourceRemoteUrl) {
        sourceRemoteUrl = (await (0, enquirer_1.prompt)([
            {
                type: 'input',
                name: 'sourceRemoteUrl',
                message: 'What is the URL of the repository you want to import? (This can be a local git repository or a git remote URL)',
                required: true,
            },
        ])).sourceRemoteUrl;
    }
    try {
        const maybeLocalDirectory = await (0, promises_1.stat)(sourceRemoteUrl);
        if (maybeLocalDirectory.isDirectory()) {
            sourceRemoteUrl = (0, path_1.resolve)(sourceRemoteUrl);
        }
    }
    catch (e) {
        // It's a remote url
    }
    const sourceRepoPath = (0, path_1.join)(tempImportDirectory, 'repo');
    const spinner = createSpinner(`Cloning ${sourceRemoteUrl} into a temporary directory: ${sourceRepoPath}`).start();
    try {
        await (0, promises_1.rm)(tempImportDirectory, { recursive: true });
    }
    catch { }
    await (0, promises_1.mkdir)(tempImportDirectory, { recursive: true });
    let sourceGitClient;
    try {
        sourceGitClient = await (0, git_utils_1.cloneFromUpstream)(sourceRemoteUrl, sourceRepoPath, {
            originName: importRemoteName,
        });
    }
    catch (e) {
        spinner.fail(`Failed to clone ${sourceRemoteUrl} into ${sourceRepoPath}`);
        let errorMessage = `Failed to clone ${sourceRemoteUrl} into ${sourceRepoPath}. Please double check the remote and try again.\n${e.message}`;
        throw new Error(errorMessage);
    }
    spinner.succeed(`Cloned into ${sourceRepoPath}`);
    if (!ref) {
        const branchChoices = await sourceGitClient.listBranches();
        ref = (await (0, enquirer_1.prompt)([
            {
                type: 'autocomplete',
                name: 'ref',
                message: `Which branch do you want to import?`,
                choices: branchChoices,
                /**
                 * Limit the number of choices so that it fits on screen
                 */
                limit: process.stdout.rows - 3,
                required: true,
            },
        ])).ref;
    }
    if (!source) {
        source = (await (0, enquirer_1.prompt)([
            {
                type: 'input',
                name: 'source',
                message: `Which directory do you want to import into this workspace? (leave blank to import the entire repository)`,
            },
        ])).source;
    }
    if (!destination) {
        destination = (await (0, enquirer_1.prompt)([
            {
                type: 'input',
                name: 'destination',
                message: 'Where in this workspace should the code be imported into?',
                required: true,
            },
        ])).destination;
    }
    const absSource = (0, path_1.join)(sourceRepoPath, source);
    const absDestination = (0, path_1.join)(process.cwd(), destination);
    try {
        await (0, promises_1.stat)(absSource);
    }
    catch (e) {
        throw new Error(`The source directory ${source} does not exist in ${sourceRemoteUrl}. Please double check to make sure it exists.`);
    }
    const destinationGitClient = new git_utils_1.GitRepository(process.cwd());
    await assertDestinationEmpty(destinationGitClient, absDestination);
    const tempImportBranch = getTempImportBranch(ref);
    const packageManager = (0, package_manager_1.detectPackageManager)(workspace_root_1.workspaceRoot);
    const originalPackageWorkspaces = await (0, needs_install_1.getPackagesInPackageManagerWorkspace)(packageManager);
    const relativeDestination = (0, path_1.relative)(destinationGitClient.root, absDestination);
    await (0, prepare_source_repo_1.prepareSourceRepo)(sourceGitClient, ref, source, relativeDestination, tempImportBranch, sourceRemoteUrl, importRemoteName);
    await createTemporaryRemote(destinationGitClient, (0, path_1.join)(sourceRepoPath, '.git'), importRemoteName);
    await (0, merge_remote_source_1.mergeRemoteSource)(destinationGitClient, sourceRemoteUrl, tempImportBranch, destination, importRemoteName, ref);
    spinner.start('Cleaning up temporary files and remotes');
    await (0, promises_1.rm)(tempImportDirectory, { recursive: true });
    await destinationGitClient.deleteGitRemote(importRemoteName);
    spinner.succeed('Cleaned up temporary files and remotes');
    const pmc = (0, package_manager_1.getPackageManagerCommand)();
    const nxJson = (0, nx_json_1.readNxJson)(workspace_root_1.workspaceRoot);
    (0, workspace_context_1.resetWorkspaceContext)();
    const { plugins, updatePackageScripts } = await (0, init_v2_1.detectPlugins)(nxJson, options.interactive);
    if (plugins.length > 0) {
        output_1.output.log({ title: 'Installing Plugins' });
        (0, init_v2_1.installPlugins)(workspace_root_1.workspaceRoot, plugins, pmc, updatePackageScripts);
        await destinationGitClient.amendCommit();
    }
    else if (await (0, needs_install_1.needsInstall)(packageManager, originalPackageWorkspaces)) {
        output_1.output.log({
            title: 'Installing dependencies for imported code',
        });
        (0, utils_1.runInstall)(workspace_root_1.workspaceRoot, (0, package_manager_1.getPackageManagerCommand)(packageManager));
        await destinationGitClient.amendCommit();
    }
    console.log(await destinationGitClient.showStat());
    output_1.output.log({
        title: `Merging these changes into ${(0, command_line_utils_1.getBaseRef)(nxJson)}`,
        bodyLines: [
            `MERGE these changes when merging these changes.`,
            `Do NOT squash and do NOT rebase these changes when merging these changes.`,
            `If you would like to UNDO these changes, run "git reset HEAD~1 --hard"`,
        ],
    });
}
async function assertDestinationEmpty(gitClient, absDestination) {
    const files = await gitClient.getGitFiles(absDestination);
    if (files.length > 0) {
        throw new Error(`Destination directory ${absDestination} is not empty. Please make sure it is empty before importing into it.`);
    }
}
function getTempImportBranch(sourceBranch) {
    return `__nx_tmp_import__/${sourceBranch}`;
}
async function createTemporaryRemote(destinationGitClient, sourceRemoteUrl, remoteName) {
    try {
        await destinationGitClient.deleteGitRemote(remoteName);
    }
    catch { }
    await destinationGitClient.addGitRemote(remoteName, sourceRemoteUrl);
    await destinationGitClient.fetch(remoteName);
}
