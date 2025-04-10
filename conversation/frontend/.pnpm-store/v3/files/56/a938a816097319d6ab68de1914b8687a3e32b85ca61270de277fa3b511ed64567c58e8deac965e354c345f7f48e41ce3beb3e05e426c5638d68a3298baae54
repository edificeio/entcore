"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.prepareSourceRepo = prepareSourceRepo;
const createSpinner = require("ora");
const path_1 = require("path");
const promises_1 = require("node:fs/promises");
async function prepareSourceRepo(gitClient, ref, source, relativeDestination, tempImportBranch, sourceRemoteUrl, originName) {
    const spinner = createSpinner().start(`Fetching ${ref} from ${sourceRemoteUrl}`);
    await gitClient.addFetchRemote(originName, ref);
    await gitClient.fetch(originName, ref);
    spinner.succeed(`Fetched ${ref} from ${sourceRemoteUrl}`);
    spinner.start(`Checking out a temporary branch, ${tempImportBranch} based on ${ref}`);
    await gitClient.checkout(tempImportBranch, {
        new: true,
        base: `${originName}/${ref}`,
    });
    spinner.succeed(`Created a ${tempImportBranch} branch based on ${ref}`);
    const relativeSourceDir = (0, path_1.relative)(gitClient.root, (0, path_1.join)(gitClient.root, source));
    const destinationInSource = (0, path_1.join)(gitClient.root, relativeDestination);
    spinner.start(`Moving files and git history to ${destinationInSource}`);
    if (relativeSourceDir === '') {
        const files = await gitClient.getGitFiles('.');
        try {
            await (0, promises_1.rm)(destinationInSource, {
                recursive: true,
            });
        }
        catch { }
        await (0, promises_1.mkdir)(destinationInSource, { recursive: true });
        const gitignores = new Set();
        for (const file of files) {
            if ((0, path_1.basename)(file) === '.gitignore') {
                gitignores.add(file);
                continue;
            }
            spinner.start(`Moving files and git history to ${destinationInSource}: ${file}`);
            const newPath = (0, path_1.join)(destinationInSource, file);
            await (0, promises_1.mkdir)((0, path_1.dirname)(newPath), { recursive: true });
            try {
                await gitClient.move(file, newPath);
            }
            catch {
                await wait(100);
                await gitClient.move(file, newPath);
            }
        }
        await gitClient.commit(`chore(repo): move ${source} to ${relativeDestination} to prepare to be imported`);
        for (const gitignore of gitignores) {
            await gitClient.move(gitignore, (0, path_1.join)(destinationInSource, gitignore));
        }
        await gitClient.amendCommit();
        for (const gitignore of gitignores) {
            await (0, promises_1.copyFile)((0, path_1.join)(destinationInSource, gitignore), (0, path_1.join)(gitClient.root, gitignore));
        }
    }
    else {
        let needsSquash = false;
        const needsMove = destinationInSource !== (0, path_1.join)(gitClient.root, source);
        if (needsMove) {
            try {
                await (0, promises_1.rm)(destinationInSource, {
                    recursive: true,
                });
                await gitClient.commit('chore(repo): prepare for import');
                needsSquash = true;
            }
            catch { }
            await (0, promises_1.mkdir)(destinationInSource, { recursive: true });
        }
        const files = await gitClient.getGitFiles('.');
        for (const file of files) {
            if (file === '.gitignore') {
                continue;
            }
            spinner.start(`Moving files and git history to ${destinationInSource}: ${file}`);
            if (!(0, path_1.relative)(source, file).startsWith('..')) {
                if (needsMove) {
                    const newPath = (0, path_1.join)(destinationInSource, file);
                    await (0, promises_1.mkdir)((0, path_1.dirname)(newPath), { recursive: true });
                    try {
                        await gitClient.move(file, newPath);
                    }
                    catch {
                        await wait(100);
                        await gitClient.move(file, newPath);
                    }
                }
            }
            else {
                await (0, promises_1.rm)((0, path_1.join)(gitClient.root, file), {
                    recursive: true,
                });
            }
        }
        await gitClient.commit('chore(repo): prepare for import 2');
        if (needsSquash) {
            await gitClient.squashLastTwoCommits();
        }
    }
    spinner.succeed(`${sourceRemoteUrl} has been prepared to be imported into this workspace on a temporary branch: ${tempImportBranch} in ${gitClient.root}`);
}
function wait(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}
