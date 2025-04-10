import { ExecSyncOptions } from 'child_process';
export declare function cloneFromUpstream(url: string, destination: string, { originName }?: {
    originName: string;
}): Promise<GitRepository>;
export declare class GitRepository {
    private directory;
    root: string;
    constructor(directory: string);
    getGitRootPath(cwd: string): string;
    addFetchRemote(remoteName: string, branch: string): Promise<string>;
    private execAsync;
    showStat(): Promise<string>;
    listBranches(): Promise<string[]>;
    getGitFiles(path: string): Promise<string[]>;
    reset(ref: string): Promise<string>;
    squashLastTwoCommits(): Promise<string>;
    mergeUnrelatedHistories(ref: string, message: string): Promise<string>;
    fetch(remote: string, ref?: string): Promise<string>;
    checkout(branch: string, opts: {
        new: boolean;
        base: string;
    }): Promise<string>;
    move(path: string, destination: string): Promise<string>;
    push(ref: string, remoteName: string): Promise<string>;
    commit(message: string): Promise<string>;
    amendCommit(): Promise<string>;
    deleteGitRemote(name: string): Promise<string>;
    deleteBranch(branch: string): Promise<string>;
    addGitRemote(name: string, url: string): Promise<string>;
}
/**
 * This is used by the squash editor script to update the rebase file.
 */
export declare function updateRebaseFile(contents: string): string;
export declare function fetchGitRemote(name: string, branch: string, execOptions: ExecSyncOptions): string | Buffer;
export declare function getGithubSlugOrNull(): string | null;
export declare function extractUserAndRepoFromGitHubUrl(gitRemotes: string): string | null;
export declare function commitChanges(commitMessage: string, directory?: string): string | null;
export declare function getLatestCommitSha(): string | null;
