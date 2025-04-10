"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getSyncGeneratorChanges = getSyncGeneratorChanges;
exports.flushSyncGeneratorChanges = flushSyncGeneratorChanges;
exports.collectAllRegisteredSyncGenerators = collectAllRegisteredSyncGenerators;
exports.runSyncGenerator = runSyncGenerator;
exports.collectRegisteredTaskSyncGenerators = collectRegisteredTaskSyncGenerators;
exports.collectRegisteredGlobalSyncGenerators = collectRegisteredGlobalSyncGenerators;
exports.syncGeneratorResultsToMessageLines = syncGeneratorResultsToMessageLines;
const perf_hooks_1 = require("perf_hooks");
const generate_1 = require("../command-line/generate/generate");
const generator_utils_1 = require("../command-line/generate/generator-utils");
const nx_json_1 = require("../config/nx-json");
const client_1 = require("../daemon/client/client");
const is_on_daemon_1 = require("../daemon/is-on-daemon");
const tree_1 = require("../generators/tree");
const project_graph_1 = require("../project-graph/project-graph");
const workspace_context_1 = require("./workspace-context");
const workspace_root_1 = require("./workspace-root");
const chalk = require("chalk");
async function getSyncGeneratorChanges(generators) {
    perf_hooks_1.performance.mark('get-sync-generators-changes:start');
    let results;
    if (!client_1.daemonClient.enabled()) {
        results = await runSyncGenerators(generators);
    }
    else {
        results = await client_1.daemonClient.getSyncGeneratorChanges(generators);
    }
    perf_hooks_1.performance.mark('get-sync-generators-changes:end');
    perf_hooks_1.performance.measure('get-sync-generators-changes', 'get-sync-generators-changes:start', 'get-sync-generators-changes:end');
    return results.filter((r) => r.changes.length > 0);
}
async function flushSyncGeneratorChanges(results) {
    if ((0, is_on_daemon_1.isOnDaemon)() || !client_1.daemonClient.enabled()) {
        await flushSyncGeneratorChangesToDisk(results);
    }
    else {
        await client_1.daemonClient.flushSyncGeneratorChangesToDisk(results.map((r) => r.generatorName));
    }
}
async function collectAllRegisteredSyncGenerators(projectGraph) {
    if (!client_1.daemonClient.enabled()) {
        return [
            ...collectRegisteredTaskSyncGenerators(projectGraph),
            ...collectRegisteredGlobalSyncGenerators(),
        ];
    }
    return await client_1.daemonClient.getRegisteredSyncGenerators();
}
async function runSyncGenerator(tree, generatorSpecifier, projects) {
    perf_hooks_1.performance.mark(`run-sync-generator:${generatorSpecifier}:start`);
    const { collection, generator } = (0, generate_1.parseGeneratorString)(generatorSpecifier);
    const { implementationFactory } = (0, generator_utils_1.getGeneratorInformation)(collection, generator, workspace_root_1.workspaceRoot, projects);
    const implementation = implementationFactory();
    const result = await implementation(tree);
    let callback;
    let outOfSyncMessage;
    if (result && typeof result === 'object') {
        callback = result.callback;
        outOfSyncMessage = result.outOfSyncMessage;
    }
    perf_hooks_1.performance.mark(`run-sync-generator:${generatorSpecifier}:end`);
    perf_hooks_1.performance.measure(`run-sync-generator:${generatorSpecifier}`, `run-sync-generator:${generatorSpecifier}:start`, `run-sync-generator:${generatorSpecifier}:end`);
    return {
        changes: tree.listChanges(),
        generatorName: generatorSpecifier,
        callback,
        outOfSyncMessage,
    };
}
function collectRegisteredTaskSyncGenerators(projectGraph) {
    const taskSyncGenerators = new Set();
    for (const { data: { targets }, } of Object.values(projectGraph.nodes)) {
        if (!targets) {
            continue;
        }
        for (const target of Object.values(targets)) {
            if (!target.syncGenerators) {
                continue;
            }
            for (const generator of target.syncGenerators) {
                taskSyncGenerators.add(generator);
            }
        }
    }
    return taskSyncGenerators;
}
function collectRegisteredGlobalSyncGenerators(nxJson = (0, nx_json_1.readNxJson)()) {
    const globalSyncGenerators = new Set();
    if (!nxJson.sync?.globalGenerators?.length) {
        return globalSyncGenerators;
    }
    for (const generator of nxJson.sync.globalGenerators) {
        globalSyncGenerators.add(generator);
    }
    return globalSyncGenerators;
}
function syncGeneratorResultsToMessageLines(results) {
    const messageLines = [];
    for (const result of results) {
        messageLines.push(`The ${chalk.bold(result.generatorName)} sync generator identified ${chalk.bold(result.changes.length)} file${result.changes.length === 1 ? '' : 's'} in the workspace that ${result.changes.length === 1 ? 'is' : 'are'} out of sync${result.outOfSyncMessage ? ':' : '.'}`);
        if (result.outOfSyncMessage) {
            messageLines.push(result.outOfSyncMessage);
        }
    }
    return messageLines;
}
async function runSyncGenerators(generators) {
    const tree = new tree_1.FsTree(workspace_root_1.workspaceRoot, false, 'running sync generators');
    const projectGraph = await (0, project_graph_1.createProjectGraphAsync)();
    const { projects } = (0, project_graph_1.readProjectsConfigurationFromProjectGraph)(projectGraph);
    const results = [];
    for (const generator of generators) {
        const result = await runSyncGenerator(tree, generator, projects);
        results.push(result);
    }
    return results;
}
async function flushSyncGeneratorChangesToDisk(results) {
    perf_hooks_1.performance.mark('flush-sync-generator-changes-to-disk:start');
    const { changes, createdFiles, updatedFiles, deletedFiles, callbacks } = processSyncGeneratorResults(results);
    // Write changes to disk
    (0, tree_1.flushChanges)(workspace_root_1.workspaceRoot, changes);
    // Run the callbacks
    if (callbacks.length) {
        for (const callback of callbacks) {
            await callback();
        }
    }
    // Update the context files
    await (0, workspace_context_1.updateContextWithChangedFiles)(workspace_root_1.workspaceRoot, createdFiles, updatedFiles, deletedFiles);
    perf_hooks_1.performance.mark('flush-sync-generator-changes-to-disk:end');
    perf_hooks_1.performance.measure('flush sync generator changes to disk', 'flush-sync-generator-changes-to-disk:start', 'flush-sync-generator-changes-to-disk:end');
}
function processSyncGeneratorResults(results) {
    const changes = [];
    const createdFiles = [];
    const updatedFiles = [];
    const deletedFiles = [];
    const callbacks = [];
    for (const result of results) {
        if (result.callback) {
            callbacks.push(result.callback);
        }
        for (const change of result.changes) {
            changes.push(change);
            if (change.type === 'CREATE') {
                createdFiles.push(change.path);
            }
            else if (change.type === 'UPDATE') {
                updatedFiles.push(change.path);
            }
            else if (change.type === 'DELETE') {
                deletedFiles.push(change.path);
            }
        }
    }
    return { changes, createdFiles, updatedFiles, deletedFiles, callbacks };
}
