"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.syncHandler = syncHandler;
const ora = require("ora");
const project_graph_1 = require("../../project-graph/project-graph");
const output_1 = require("../../utils/output");
const params_1 = require("../../utils/params");
const sync_generators_1 = require("../../utils/sync-generators");
const chalk = require("chalk");
function syncHandler(options) {
    if (options.verbose) {
        process.env.NX_VERBOSE_LOGGING = 'true';
    }
    const isVerbose = process.env.NX_VERBOSE_LOGGING === 'true';
    return (0, params_1.handleErrors)(isVerbose, async () => {
        const projectGraph = await (0, project_graph_1.createProjectGraphAsync)();
        const syncGenerators = await (0, sync_generators_1.collectAllRegisteredSyncGenerators)(projectGraph);
        const results = await (0, sync_generators_1.getSyncGeneratorChanges)(syncGenerators);
        if (!results.length) {
            output_1.output.success({
                title: options.check
                    ? 'The workspace is up to date'
                    : 'The workspace is already up to date',
                bodyLines: syncGenerators.map((generator) => `The ${chalk.bold(generator)} sync generator didn't identify any files in the workspace that are out of sync.`),
            });
            return 0;
        }
        if (options.check) {
            output_1.output.error({
                title: `The workspace is out of sync`,
                bodyLines: (0, sync_generators_1.syncGeneratorResultsToMessageLines)(results),
            });
            return 1;
        }
        output_1.output.warn({
            title: `The workspace is out of sync`,
            bodyLines: (0, sync_generators_1.syncGeneratorResultsToMessageLines)(results),
        });
        const spinner = ora('Syncing the workspace...');
        spinner.start();
        await (0, sync_generators_1.flushSyncGeneratorChanges)(results);
        spinner.succeed(`The workspace was synced successfully!

Please make sure to commit the changes to your repository.
`);
        return 0;
    });
}
