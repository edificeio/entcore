"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.yargsImportCommand = void 0;
const documentation_1 = require("../yargs-utils/documentation");
const shared_options_1 = require("../yargs-utils/shared-options");
const params_1 = require("../../utils/params");
exports.yargsImportCommand = {
    command: 'import [sourceRemoteUrl] [destination]',
    describe: false,
    builder: (yargs) => (0, documentation_1.linkToNxDevAndExamples)((0, shared_options_1.withVerbose)(yargs
        .positional('sourceRemoteUrl', {
        type: 'string',
        description: 'The remote URL of the source to import',
    })
        .positional('destination', {
        type: 'string',
        description: 'The directory in the current workspace to import into',
    })
        .option('source', {
        type: 'string',
        description: 'The directory in the source repository to import from',
    })
        .option('ref', {
        type: 'string',
        description: 'The branch from the source repository to import',
    })
        .option('interactive', {
        type: 'boolean',
        description: 'Interactive mode',
        default: true,
    })), 'import'),
    handler: async (args) => {
        const exitCode = await (0, params_1.handleErrors)(args.verbose ?? process.env.NX_VERBOSE_LOGGING === 'true', async () => {
            return (await Promise.resolve().then(() => require('./import'))).importHandler(args);
        });
        process.exit(exitCode);
    },
};
