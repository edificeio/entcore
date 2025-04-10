"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.yargsSyncCheckCommand = exports.yargsSyncCommand = void 0;
exports.yargsSyncCommand = {
    command: 'sync',
    describe: false,
    builder: (yargs) => yargs.option('verbose', {
        type: 'boolean',
        description: 'Prints additional information about the commands (e.g., stack traces)',
    }),
    handler: async (args) => {
        process.exit(await Promise.resolve().then(() => require('./sync')).then((m) => m.syncHandler(args)));
    },
};
exports.yargsSyncCheckCommand = {
    command: 'sync:check',
    describe: false,
    builder: (yargs) => yargs.option('verbose', {
        type: 'boolean',
        description: 'Prints additional information about the commands (e.g., stack traces)',
    }),
    handler: async (args) => {
        process.exit(await Promise.resolve().then(() => require('./sync')).then((m) => m.syncHandler({ ...args, check: true })));
    },
};
