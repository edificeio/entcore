"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.runCommand = runCommand;
exports.invokeTasksRunner = invokeTasksRunner;
exports.getRunner = getRunner;
exports.getRunnerOptions = getRunnerOptions;
const enquirer_1 = require("enquirer");
const ora = require("ora");
const path_1 = require("path");
const nx_json_1 = require("../config/nx-json");
const client_1 = require("../daemon/client/client");
const create_task_hasher_1 = require("../hasher/create-task-hasher");
const hash_task_1 = require("../hasher/hash-task");
const project_graph_1 = require("../project-graph/project-graph");
const fileutils_1 = require("../utils/fileutils");
const is_ci_1 = require("../utils/is-ci");
const nx_cloud_utils_1 = require("../utils/nx-cloud-utils");
const output_1 = require("../utils/output");
const params_1 = require("../utils/params");
const sync_generators_1 = require("../utils/sync-generators");
const workspace_root_1 = require("../utils/workspace-root");
const create_task_graph_1 = require("./create-task-graph");
const life_cycle_1 = require("./life-cycle");
const dynamic_run_many_terminal_output_life_cycle_1 = require("./life-cycles/dynamic-run-many-terminal-output-life-cycle");
const dynamic_run_one_terminal_output_life_cycle_1 = require("./life-cycles/dynamic-run-one-terminal-output-life-cycle");
const static_run_many_terminal_output_life_cycle_1 = require("./life-cycles/static-run-many-terminal-output-life-cycle");
const static_run_one_terminal_output_life_cycle_1 = require("./life-cycles/static-run-one-terminal-output-life-cycle");
const store_run_information_life_cycle_1 = require("./life-cycles/store-run-information-life-cycle");
const task_history_life_cycle_1 = require("./life-cycles/task-history-life-cycle");
const task_profiling_life_cycle_1 = require("./life-cycles/task-profiling-life-cycle");
const task_timings_life_cycle_1 = require("./life-cycles/task-timings-life-cycle");
const task_graph_utils_1 = require("./task-graph-utils");
const utils_1 = require("./utils");
const chalk = require("chalk");
async function getTerminalOutputLifeCycle(initiatingProject, projectNames, tasks, nxArgs, nxJson, overrides) {
    const { runnerOptions } = getRunner(nxArgs, nxJson);
    const isRunOne = initiatingProject != null;
    const useDynamicOutput = shouldUseDynamicLifeCycle(tasks, runnerOptions, nxArgs.outputStyle);
    const overridesWithoutHidden = { ...overrides };
    delete overridesWithoutHidden['__overrides_unparsed__'];
    if (isRunOne) {
        if (useDynamicOutput) {
            return await (0, dynamic_run_one_terminal_output_life_cycle_1.createRunOneDynamicOutputRenderer)({
                initiatingProject,
                tasks,
                args: nxArgs,
                overrides: overridesWithoutHidden,
            });
        }
        return {
            lifeCycle: new static_run_one_terminal_output_life_cycle_1.StaticRunOneTerminalOutputLifeCycle(initiatingProject, projectNames, tasks, nxArgs),
            renderIsDone: Promise.resolve(),
        };
    }
    else {
        if (useDynamicOutput) {
            return await (0, dynamic_run_many_terminal_output_life_cycle_1.createRunManyDynamicOutputRenderer)({
                projectNames,
                tasks,
                args: nxArgs,
                overrides: overridesWithoutHidden,
            });
        }
        else {
            return {
                lifeCycle: new static_run_many_terminal_output_life_cycle_1.StaticRunManyTerminalOutputLifeCycle(projectNames, tasks, nxArgs, overridesWithoutHidden),
                renderIsDone: Promise.resolve(),
            };
        }
    }
}
function createTaskGraphAndRunValidations(projectGraph, extraTargetDependencies, projectNames, nxArgs, overrides, extraOptions) {
    const taskGraph = (0, create_task_graph_1.createTaskGraph)(projectGraph, extraTargetDependencies, projectNames, nxArgs.targets, nxArgs.configuration, overrides, extraOptions.excludeTaskDependencies);
    const cycle = (0, task_graph_utils_1.findCycle)(taskGraph);
    if (cycle) {
        if (process.env.NX_IGNORE_CYCLES === 'true' || nxArgs.nxIgnoreCycles) {
            output_1.output.warn({
                title: `The task graph has a circular dependency`,
                bodyLines: [`${cycle.join(' --> ')}`],
            });
            (0, task_graph_utils_1.makeAcyclic)(taskGraph);
        }
        else {
            output_1.output.error({
                title: `Could not execute command because the task graph has a circular dependency`,
                bodyLines: [`${cycle.join(' --> ')}`],
            });
            process.exit(1);
        }
    }
    // validate that no atomized tasks like e2e-ci are used without Nx Cloud
    if (!(0, nx_cloud_utils_1.isNxCloudUsed)((0, nx_json_1.readNxJson)()) &&
        !process.env['NX_SKIP_ATOMIZER_VALIDATION']) {
        (0, task_graph_utils_1.validateNoAtomizedTasks)(taskGraph, projectGraph);
    }
    return taskGraph;
}
async function runCommand(projectsToRun, currentProjectGraph, { nxJson }, nxArgs, overrides, initiatingProject, extraTargetDependencies, extraOptions) {
    const status = await (0, params_1.handleErrors)(process.env.NX_VERBOSE_LOGGING === 'true', async () => {
        const projectNames = projectsToRun.map((t) => t.name);
        const { projectGraph, taskGraph } = await ensureWorkspaceIsInSyncAndGetGraphs(currentProjectGraph, nxJson, projectNames, nxArgs, overrides, extraTargetDependencies, extraOptions);
        const tasks = Object.values(taskGraph.tasks);
        const { lifeCycle, renderIsDone } = await getTerminalOutputLifeCycle(initiatingProject, projectNames, tasks, nxArgs, nxJson, overrides);
        const status = await invokeTasksRunner({
            tasks,
            projectGraph,
            taskGraph,
            lifeCycle,
            nxJson,
            nxArgs,
            loadDotEnvFiles: extraOptions.loadDotEnvFiles,
            initiatingProject,
        });
        await renderIsDone;
        return status;
    });
    return status;
}
async function ensureWorkspaceIsInSyncAndGetGraphs(projectGraph, nxJson, projectNames, nxArgs, overrides, extraTargetDependencies, extraOptions) {
    let taskGraph = createTaskGraphAndRunValidations(projectGraph, extraTargetDependencies ?? {}, projectNames, nxArgs, overrides, extraOptions);
    // collect unique syncGenerators from the tasks
    const uniqueSyncGenerators = new Set();
    for (const { target } of Object.values(taskGraph.tasks)) {
        const { syncGenerators } = projectGraph.nodes[target.project].data.targets[target.target];
        if (!syncGenerators) {
            continue;
        }
        for (const generator of syncGenerators) {
            uniqueSyncGenerators.add(generator);
        }
    }
    if (!uniqueSyncGenerators.size) {
        // There are no sync generators registered in the tasks to run
        return { projectGraph, taskGraph };
    }
    const syncGenerators = Array.from(uniqueSyncGenerators);
    const results = await (0, sync_generators_1.getSyncGeneratorChanges)(syncGenerators);
    if (!results.length) {
        // There are no changes to sync, workspace is up to date
        return { projectGraph, taskGraph };
    }
    const outOfSyncTitle = 'The workspace is out of sync';
    const resultBodyLines = [...(0, sync_generators_1.syncGeneratorResultsToMessageLines)(results), ''];
    const fixMessage = 'You can manually run `nx sync` to update your workspace or you can set `sync.applyChanges` to `true` in your `nx.json` to apply the changes automatically when running tasks.';
    const willErrorOnCiMessage = 'Please note that this will be an error on CI.';
    if ((0, is_ci_1.isCI)() || !process.stdout.isTTY) {
        // If the user is running in CI or is running in a non-TTY environment we
        // throw an error to stop the execution of the tasks.
        throw new Error(`${outOfSyncTitle}\n${resultBodyLines.join('\n')}\n${fixMessage}`);
    }
    if (nxJson.sync?.applyChanges === false) {
        // If the user has set `sync.applyChanges` to `false` in their `nx.json`
        // we don't prompt the them and just log a warning informing them that
        // the workspace is out of sync and they have it set to not apply changes
        // automatically.
        output_1.output.warn({
            title: outOfSyncTitle,
            bodyLines: [
                ...resultBodyLines,
                'Your workspace is set to not apply changes automatically (`sync.applyChanges` is set to `false` in your `nx.json`).',
                willErrorOnCiMessage,
                fixMessage,
            ],
        });
        return { projectGraph, taskGraph };
    }
    output_1.output.warn({
        title: outOfSyncTitle,
        bodyLines: [
            ...resultBodyLines,
            nxJson.sync?.applyChanges === true
                ? 'Proceeding to sync the changes automatically (`sync.applyChanges` is set to `true` in your `nx.json`).'
                : willErrorOnCiMessage,
        ],
    });
    const applyChanges = nxJson.sync?.applyChanges === true ||
        (await promptForApplyingSyncGeneratorChanges());
    if (applyChanges) {
        const spinner = ora('Syncing the workspace...');
        spinner.start();
        // Flush sync generator changes to disk
        await (0, sync_generators_1.flushSyncGeneratorChanges)(results);
        // Re-create project graph and task graph
        projectGraph = await (0, project_graph_1.createProjectGraphAsync)();
        taskGraph = createTaskGraphAndRunValidations(projectGraph, extraTargetDependencies ?? {}, projectNames, nxArgs, overrides, extraOptions);
        if (nxJson.sync?.applyChanges === true) {
            spinner.succeed(`The workspace was synced successfully!

Please make sure to commit the changes to your repository or this will error on CI.`);
        }
        else {
            // The user was prompted and we already logged a message about erroring on CI
            // so here we just tell them to commit the changes.
            spinner.succeed(`The workspace was synced successfully!

Please make sure to commit the changes to your repository.`);
        }
    }
    else {
        output_1.output.warn({
            title: 'Syncing the workspace was skipped',
            bodyLines: [
                'This could lead to unexpected results or errors when running tasks.',
                fixMessage,
            ],
        });
    }
    return { projectGraph, taskGraph };
}
async function promptForApplyingSyncGeneratorChanges() {
    try {
        const promptConfig = {
            name: 'applyChanges',
            type: 'select',
            message: 'Would you like to sync the changes to get your worskpace up to date?',
            choices: [
                {
                    name: 'yes',
                    message: 'Yes, sync the changes and run the tasks',
                },
                {
                    name: 'no',
                    message: 'No, run the tasks without syncing the changes',
                },
            ],
            footer: () => chalk.dim('\nYou can skip this prompt by setting the `sync.applyChanges` option in your `nx.json`.'),
        };
        return await (0, enquirer_1.prompt)([promptConfig]).then(({ applyChanges }) => applyChanges === 'yes');
    }
    catch {
        process.exit(1);
    }
}
function setEnvVarsBasedOnArgs(nxArgs, loadDotEnvFiles) {
    if (nxArgs.outputStyle == 'stream' ||
        process.env.NX_BATCH_MODE === 'true' ||
        nxArgs.batch) {
        process.env.NX_STREAM_OUTPUT = 'true';
        process.env.NX_PREFIX_OUTPUT = 'true';
    }
    if (nxArgs.outputStyle == 'stream-without-prefixes') {
        process.env.NX_STREAM_OUTPUT = 'true';
    }
    if (loadDotEnvFiles) {
        process.env.NX_LOAD_DOT_ENV_FILES = 'true';
    }
}
async function invokeTasksRunner({ tasks, projectGraph, taskGraph, lifeCycle, nxJson, nxArgs, loadDotEnvFiles, initiatingProject, }) {
    setEnvVarsBasedOnArgs(nxArgs, loadDotEnvFiles);
    const { tasksRunner, runnerOptions } = getRunner(nxArgs, nxJson);
    let hasher = (0, create_task_hasher_1.createTaskHasher)(projectGraph, nxJson, runnerOptions);
    // this is used for two reasons: to fetch all remote cache hits AND
    // to submit everything that is known in advance to Nx Cloud to run in
    // a distributed fashion
    await (0, hash_task_1.hashTasksThatDoNotDependOnOutputsOfOtherTasks)(hasher, projectGraph, taskGraph, nxJson);
    const promiseOrObservable = tasksRunner(tasks, {
        ...runnerOptions,
        lifeCycle: new life_cycle_1.CompositeLifeCycle(constructLifeCycles(lifeCycle)),
    }, {
        initiatingProject: nxArgs.outputStyle === 'compact' ? null : initiatingProject,
        projectGraph,
        nxJson,
        nxArgs,
        taskGraph,
        hasher: {
            hashTask(task, taskGraph_, env) {
                if (!taskGraph_) {
                    output_1.output.warn({
                        title: `TaskGraph is now required as an argument to hashTask`,
                        bodyLines: [
                            `The TaskGraph object can be retrieved from the context`,
                            'This will result in an error in Nx 20',
                        ],
                    });
                    taskGraph_ = taskGraph;
                }
                if (!env) {
                    output_1.output.warn({
                        title: `The environment variables are now required as an argument to hashTask`,
                        bodyLines: [
                            `Please pass the environment variables used when running the task`,
                            'This will result in an error in Nx 20',
                        ],
                    });
                    env = process.env;
                }
                return hasher.hashTask(task, taskGraph_, env);
            },
            hashTasks(task, taskGraph_, env) {
                if (!taskGraph_) {
                    output_1.output.warn({
                        title: `TaskGraph is now required as an argument to hashTasks`,
                        bodyLines: [
                            `The TaskGraph object can be retrieved from the context`,
                            'This will result in an error in Nx 20',
                        ],
                    });
                    taskGraph_ = taskGraph;
                }
                if (!env) {
                    output_1.output.warn({
                        title: `The environment variables are now required as an argument to hashTasks`,
                        bodyLines: [
                            `Please pass the environment variables used when running the tasks`,
                            'This will result in an error in Nx 20',
                        ],
                    });
                    env = process.env;
                }
                return hasher.hashTasks(task, taskGraph_, env);
            },
        },
        daemon: client_1.daemonClient,
    });
    let anyFailures;
    if (promiseOrObservable.subscribe) {
        anyFailures = await anyFailuresInObservable(promiseOrObservable);
    }
    else {
        // simply await the promise
        anyFailures = await anyFailuresInPromise(promiseOrObservable);
    }
    return anyFailures ? 1 : 0;
}
function constructLifeCycles(lifeCycle) {
    const lifeCycles = [];
    lifeCycles.push(new store_run_information_life_cycle_1.StoreRunInformationLifeCycle());
    lifeCycles.push(lifeCycle);
    if (process.env.NX_PERF_LOGGING === 'true') {
        lifeCycles.push(new task_timings_life_cycle_1.TaskTimingsLifeCycle());
    }
    if (process.env.NX_PROFILE) {
        lifeCycles.push(new task_profiling_life_cycle_1.TaskProfilingLifeCycle(process.env.NX_PROFILE));
    }
    if (!(0, nx_cloud_utils_1.isNxCloudUsed)((0, nx_json_1.readNxJson)())) {
        lifeCycles.push(new task_history_life_cycle_1.TaskHistoryLifeCycle());
    }
    return lifeCycles;
}
function mergeTargetDependencies(defaults, deps) {
    const res = {};
    Object.keys(defaults ?? {}).forEach((k) => {
        res[k] = defaults[k].dependsOn;
    });
    if (deps) {
        Object.keys(deps).forEach((k) => {
            if (res[k]) {
                res[k] = [...res[k], deps[k]];
            }
            else {
                res[k] = deps[k];
            }
        });
        return res;
    }
}
async function anyFailuresInPromise(promise) {
    return Object.values(await promise).some((v) => v === 'failure' || v === 'skipped');
}
async function anyFailuresInObservable(obs) {
    return await new Promise((res) => {
        let anyFailures = false;
        obs.subscribe((t) => {
            if (!t.success) {
                anyFailures = true;
            }
        }, (error) => {
            output_1.output.error({
                title: 'Unhandled error in task executor',
            });
            console.error(error);
            res(true);
        }, () => {
            res(anyFailures);
        });
    });
}
function shouldUseDynamicLifeCycle(tasks, options, outputStyle) {
    if (process.env.NX_BATCH_MODE === 'true' ||
        process.env.NX_VERBOSE_LOGGING === 'true' ||
        process.env.NX_TASKS_RUNNER_DYNAMIC_OUTPUT === 'false') {
        return false;
    }
    if (!process.stdout.isTTY)
        return false;
    if ((0, is_ci_1.isCI)())
        return false;
    if (outputStyle === 'static' || outputStyle === 'stream')
        return false;
    return !tasks.find((t) => (0, utils_1.shouldStreamOutput)(t, null));
}
function loadTasksRunner(modulePath) {
    try {
        const maybeTasksRunner = require(modulePath);
        // to support both babel and ts formats
        return 'default' in maybeTasksRunner
            ? maybeTasksRunner.default
            : maybeTasksRunner;
    }
    catch (e) {
        if (e.code === 'MODULE_NOT_FOUND' &&
            (modulePath === 'nx-cloud' || modulePath === '@nrwl/nx-cloud')) {
            return require('../nx-cloud/nx-cloud-tasks-runner-shell')
                .nxCloudTasksRunnerShell;
        }
        throw e;
    }
}
function getRunner(nxArgs, nxJson) {
    let runner = nxArgs.runner;
    runner = runner || 'default';
    if (runner !== 'default' && !nxJson.tasksRunnerOptions?.[runner]) {
        throw new Error(`Could not find runner configuration for ${runner}`);
    }
    const modulePath = getTasksRunnerPath(runner, nxJson);
    try {
        const tasksRunner = loadTasksRunner(modulePath);
        return {
            tasksRunner,
            runnerOptions: getRunnerOptions(runner, nxJson, nxArgs, modulePath === 'nx-cloud'),
        };
    }
    catch {
        throw new Error(`Could not find runner configuration for ${runner}`);
    }
}
function getTasksRunnerPath(runner, nxJson) {
    let modulePath = nxJson.tasksRunnerOptions?.[runner]?.runner;
    if (modulePath) {
        if ((0, fileutils_1.isRelativePath)(modulePath)) {
            return (0, path_1.join)(workspace_root_1.workspaceRoot, modulePath);
        }
        return modulePath;
    }
    const isCloudRunner = 
    // No tasksRunnerOptions for given --runner
    nxJson.nxCloudAccessToken ||
        // No runner prop in tasks runner options, check if access token is set.
        nxJson.tasksRunnerOptions?.[runner]?.options?.accessToken ||
        // Cloud access token specified in env var.
        process.env.NX_CLOUD_ACCESS_TOKEN ||
        // Nx Cloud Id specified in nxJson
        nxJson.nxCloudId;
    return isCloudRunner ? 'nx-cloud' : require.resolve('./default-tasks-runner');
}
function getRunnerOptions(runner, nxJson, nxArgs, isCloudDefault) {
    const defaultCacheableOperations = [];
    for (const key in nxJson.targetDefaults) {
        if (nxJson.targetDefaults[key].cache) {
            defaultCacheableOperations.push(key);
        }
    }
    const result = {
        ...nxJson.tasksRunnerOptions?.[runner]?.options,
        ...nxArgs,
    };
    // NOTE: we don't pull from env here because the cloud package
    // supports it within nx-cloud's implementation. We could
    // normalize it here, and that may make more sense, but
    // leaving it as is for now.
    if (nxJson.nxCloudAccessToken && isCloudDefault) {
        result.accessToken ??= nxJson.nxCloudAccessToken;
    }
    if (nxJson.nxCloudId && isCloudDefault) {
        result.nxCloudId ??= nxJson.nxCloudId;
    }
    if (nxJson.nxCloudUrl && isCloudDefault) {
        result.url ??= nxJson.nxCloudUrl;
    }
    if (nxJson.nxCloudEncryptionKey && isCloudDefault) {
        result.encryptionKey ??= nxJson.nxCloudEncryptionKey;
    }
    if (nxJson.parallel) {
        result.parallel ??= nxJson.parallel;
    }
    if (nxJson.cacheDirectory) {
        result.cacheDirectory ??= nxJson.cacheDirectory;
    }
    if (defaultCacheableOperations.length) {
        result.cacheableOperations ??= [];
        result.cacheableOperations = result.cacheableOperations.concat(defaultCacheableOperations);
    }
    if (nxJson.useDaemonProcess !== undefined) {
        result.useDaemonProcess ??= nxJson.useDaemonProcess;
    }
    return result;
}
