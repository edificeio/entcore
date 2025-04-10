"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getCachedSyncGeneratorChanges = getCachedSyncGeneratorChanges;
exports.flushSyncGeneratorChangesToDisk = flushSyncGeneratorChangesToDisk;
exports.collectAndScheduleSyncGenerators = collectAndScheduleSyncGenerators;
exports.getCachedRegisteredSyncGenerators = getCachedRegisteredSyncGenerators;
const nx_json_1 = require("../../config/nx-json");
const tree_1 = require("../../generators/tree");
const file_hasher_1 = require("../../hasher/file-hasher");
const project_graph_1 = require("../../project-graph/project-graph");
const sync_generators_1 = require("../../utils/sync-generators");
const workspace_root_1 = require("../../utils/workspace-root");
const logger_1 = require("./logger");
const project_graph_incremental_recomputation_1 = require("./project-graph-incremental-recomputation");
const syncGeneratorsCacheResultPromises = new Map();
let registeredTaskSyncGenerators = new Set();
let registeredGlobalSyncGenerators = new Set();
const scheduledGenerators = new Set();
let waitPeriod = 100;
let registeredSyncGenerators;
let scheduledTimeoutId;
let storedProjectGraphHash;
let storedNxJsonHash;
const log = (...messageParts) => {
    logger_1.serverLogger.log('[SYNC]:', ...messageParts);
};
// TODO(leo): check conflicts and reuse the Tree where possible
async function getCachedSyncGeneratorChanges(generators) {
    try {
        log('get sync generators changes on demand', generators);
        // this is invoked imperatively, so we clear any scheduled run
        if (scheduledTimeoutId) {
            log('clearing scheduled run');
            clearTimeout(scheduledTimeoutId);
            scheduledTimeoutId = undefined;
        }
        // reset the wait time
        waitPeriod = 100;
        let projects;
        let errored = false;
        const getProjectsConfigurations = async () => {
            if (projects || errored) {
                return projects;
            }
            const { projectGraph, error } = await (0, project_graph_incremental_recomputation_1.getCachedSerializedProjectGraphPromise)();
            projects = projectGraph
                ? (0, project_graph_1.readProjectsConfigurationFromProjectGraph)(projectGraph).projects
                : null;
            errored = error !== undefined;
            return projects;
        };
        return (await Promise.all(generators.map(async (generator) => {
            if (scheduledGenerators.has(generator) ||
                !syncGeneratorsCacheResultPromises.has(generator)) {
                // it's scheduled to run (there are pending changes to process) or
                // it's not scheduled and there's no cached result, so run it
                const projects = await getProjectsConfigurations();
                if (projects) {
                    log(generator, 'already scheduled or not cached, running it now');
                    runGenerator(generator, projects);
                }
                else {
                    log(generator, 'already scheduled or not cached, project graph errored');
                    /**
                     * This should never happen. This is invoked imperatively, and by
                     * the time it is invoked, the project graph would have already
                     * been requested. If it errored, it would have been reported and
                     * this wouldn't have been invoked. We handle it just in case.
                     *
                     * Since the project graph would be reported by the relevant
                     * handlers separately, we just ignore the error, don't cache
                     * any result and return an empty result, the next time this is
                     * invoked the process will repeat until it eventually recovers
                     * when the project graph is fixed.
                     */
                    return Promise.resolve({ changes: [], generatorName: generator });
                }
            }
            else {
                log(generator, 'not scheduled and has cached result, returning cached result');
            }
            return syncGeneratorsCacheResultPromises.get(generator);
        }))).flat();
    }
    catch (e) {
        console.error(e);
        syncGeneratorsCacheResultPromises.clear();
        return [];
    }
}
async function flushSyncGeneratorChangesToDisk(generators) {
    log('flush sync generators changes', generators);
    const results = await getCachedSyncGeneratorChanges(generators);
    for (const generator of generators) {
        syncGeneratorsCacheResultPromises.delete(generator);
    }
    await (0, sync_generators_1.flushSyncGeneratorChanges)(results);
}
function collectAndScheduleSyncGenerators(projectGraph) {
    if (!projectGraph) {
        // If the project graph is not available, we can't collect and schedule
        // sync generators. The project graph error will be reported separately.
        return;
    }
    log('collect registered sync generators');
    collectAllRegisteredSyncGenerators(projectGraph);
    // a change imply we need to re-run all the generators
    // make sure to schedule all the collected generators
    scheduledGenerators.clear();
    for (const generator of registeredSyncGenerators) {
        scheduledGenerators.add(generator);
    }
    log('scheduling:', [...scheduledGenerators]);
    if (scheduledTimeoutId) {
        // we have a scheduled run already, so we don't need to do anything
        return;
    }
    scheduledTimeoutId = setTimeout(async () => {
        scheduledTimeoutId = undefined;
        if (waitPeriod < 4000) {
            waitPeriod = waitPeriod * 2;
        }
        if (scheduledGenerators.size === 0) {
            // no generators to run
            return;
        }
        const { projects } = (0, project_graph_1.readProjectsConfigurationFromProjectGraph)(projectGraph);
        for (const generator of scheduledGenerators) {
            runGenerator(generator, projects);
        }
        await Promise.all(syncGeneratorsCacheResultPromises.values());
    }, waitPeriod);
}
async function getCachedRegisteredSyncGenerators() {
    log('get registered sync generators');
    if (!registeredSyncGenerators) {
        log('no registered sync generators, collecting them');
        const { projectGraph } = await (0, project_graph_incremental_recomputation_1.getCachedSerializedProjectGraphPromise)();
        collectAllRegisteredSyncGenerators(projectGraph);
    }
    else {
        log('registered sync generators already collected, returning them');
    }
    return [...registeredSyncGenerators];
}
function collectAllRegisteredSyncGenerators(projectGraph) {
    const projectGraphHash = hashProjectGraph(projectGraph);
    if (storedProjectGraphHash !== projectGraphHash) {
        storedProjectGraphHash = projectGraphHash;
        registeredTaskSyncGenerators =
            (0, sync_generators_1.collectRegisteredTaskSyncGenerators)(projectGraph);
    }
    else {
        log('project graph hash is the same, not collecting task sync generators');
    }
    const nxJson = (0, nx_json_1.readNxJson)();
    const nxJsonHash = (0, file_hasher_1.hashArray)(nxJson.sync?.globalGenerators?.sort() ?? []);
    if (storedNxJsonHash !== nxJsonHash) {
        storedNxJsonHash = nxJsonHash;
        registeredGlobalSyncGenerators =
            (0, sync_generators_1.collectRegisteredGlobalSyncGenerators)(nxJson);
    }
    else {
        log('nx.json hash is the same, not collecting global sync generators');
    }
    const generators = new Set([
        ...registeredTaskSyncGenerators,
        ...registeredGlobalSyncGenerators,
    ]);
    if (!registeredSyncGenerators) {
        registeredSyncGenerators = generators;
        return;
    }
    for (const generator of registeredSyncGenerators) {
        if (!generators.has(generator)) {
            registeredSyncGenerators.delete(generator);
            syncGeneratorsCacheResultPromises.delete(generator);
        }
    }
    for (const generator of generators) {
        if (!registeredSyncGenerators.has(generator)) {
            registeredSyncGenerators.add(generator);
        }
    }
}
function runGenerator(generator, projects) {
    log('running scheduled generator', generator);
    // remove it from the scheduled set
    scheduledGenerators.delete(generator);
    const tree = new tree_1.FsTree(workspace_root_1.workspaceRoot, false, `running sync generator ${generator}`);
    // run the generator and cache the result
    syncGeneratorsCacheResultPromises.set(generator, (0, sync_generators_1.runSyncGenerator)(tree, generator, projects).then((result) => {
        log(generator, 'changes:', result.changes.map((c) => c.path).join(', '));
        return result;
    }));
}
function hashProjectGraph(projectGraph) {
    const stringifiedProjects = Object.entries(projectGraph.nodes)
        .sort(([projectNameA], [projectNameB]) => projectNameA.localeCompare(projectNameB))
        .map(([projectName, projectConfig]) => `${projectName}:${JSON.stringify(projectConfig)}`);
    return (0, file_hasher_1.hashArray)(stringifiedProjects);
}
