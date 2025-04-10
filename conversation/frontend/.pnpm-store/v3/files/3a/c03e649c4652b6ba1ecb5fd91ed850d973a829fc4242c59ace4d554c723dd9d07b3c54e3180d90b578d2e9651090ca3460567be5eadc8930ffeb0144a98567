"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.releasePlanCLIHandler = void 0;
exports.createAPI = createAPI;
const enquirer_1 = require("enquirer");
const fs_extra_1 = require("fs-extra");
const node_path_1 = require("node:path");
const semver_1 = require("semver");
const tmp_1 = require("tmp");
const nx_json_1 = require("../../config/nx-json");
const file_map_utils_1 = require("../../project-graph/file-map-utils");
const project_graph_1 = require("../../project-graph/project-graph");
const output_1 = require("../../utils/output");
const params_1 = require("../../utils/params");
const config_1 = require("./config/config");
const deep_merge_json_1 = require("./config/deep-merge-json");
const filter_release_groups_1 = require("./config/filter-release-groups");
const version_plans_1 = require("./config/version-plans");
const generate_version_plan_content_1 = require("./utils/generate-version-plan-content");
const launch_editor_1 = require("./utils/launch-editor");
const print_changes_1 = require("./utils/print-changes");
const print_config_1 = require("./utils/print-config");
const releasePlanCLIHandler = (args) => (0, params_1.handleErrors)(args.verbose, () => createAPI({})(args));
exports.releasePlanCLIHandler = releasePlanCLIHandler;
function createAPI(overrideReleaseConfig) {
    return async function releasePlan(args) {
        const projectGraph = await (0, project_graph_1.createProjectGraphAsync)({ exitOnError: true });
        const nxJson = (0, nx_json_1.readNxJson)();
        const userProvidedReleaseConfig = (0, deep_merge_json_1.deepMergeJson)(nxJson.release ?? {}, overrideReleaseConfig ?? {});
        if (args.verbose) {
            process.env.NX_VERBOSE_LOGGING = 'true';
        }
        // Apply default configuration to any optional user configuration
        const { error: configError, nxReleaseConfig } = await (0, config_1.createNxReleaseConfig)(projectGraph, await (0, file_map_utils_1.createProjectFileMapUsingProjectGraph)(projectGraph), userProvidedReleaseConfig);
        if (configError) {
            return await (0, config_1.handleNxReleaseConfigError)(configError);
        }
        // --print-config exits directly as it is not designed to be combined with any other programmatic operations
        if (args.printConfig) {
            return (0, print_config_1.printConfigAndExit)({
                userProvidedReleaseConfig,
                nxReleaseConfig,
                isDebug: args.printConfig === 'debug',
            });
        }
        const { error: filterError, releaseGroups, releaseGroupToFilteredProjects, } = (0, filter_release_groups_1.filterReleaseGroups)(projectGraph, nxReleaseConfig, args.projects, args.groups);
        if (filterError) {
            output_1.output.error(filterError);
            process.exit(1);
        }
        const versionPlanBumps = {};
        const setBumpIfNotNone = (projectOrGroup, version) => {
            if (version !== 'none') {
                versionPlanBumps[projectOrGroup] = version;
            }
        };
        if (releaseGroups[0].name === config_1.IMPLICIT_DEFAULT_RELEASE_GROUP) {
            const group = releaseGroups[0];
            if (group.projectsRelationship === 'independent') {
                for (const project of group.projects) {
                    setBumpIfNotNone(project, args.bump ||
                        (await promptForVersion(`How do you want to bump the version of the project "${project}"?`)));
                }
            }
            else {
                // TODO: use project names instead of the implicit default release group name? (though this might be confusing, as users might think they can just delete one of the project bumps to change the behavior to independent versioning)
                setBumpIfNotNone(group.name, args.bump ||
                    (await promptForVersion(`How do you want to bump the versions of all projects?`)));
            }
        }
        else {
            for (const group of releaseGroups) {
                if (group.projectsRelationship === 'independent') {
                    for (const project of releaseGroupToFilteredProjects.get(group)) {
                        setBumpIfNotNone(project, args.bump ||
                            (await promptForVersion(`How do you want to bump the version of the project "${project}" within group "${group.name}"?`)));
                    }
                }
                else {
                    setBumpIfNotNone(group.name, args.bump ||
                        (await promptForVersion(`How do you want to bump the versions of the projects in the group "${group.name}"?`)));
                }
            }
        }
        if (!Object.keys(versionPlanBumps).length) {
            output_1.output.warn({
                title: 'No version bumps were selected so no version plan file was created.',
            });
            return 0;
        }
        const versionPlanName = `version-plan-${new Date().getTime()}`;
        const versionPlanMessage = args.message || (await promptForMessage(versionPlanName));
        const versionPlanFileContent = (0, generate_version_plan_content_1.generateVersionPlanContent)(versionPlanBumps, versionPlanMessage);
        const versionPlanFileName = `${versionPlanName}.md`;
        if (args.dryRun) {
            output_1.output.logSingleLine(`Would create version plan file "${versionPlanFileName}", but --dry-run was set.`);
            (0, print_changes_1.printDiff)('', versionPlanFileContent, 1);
        }
        else {
            output_1.output.logSingleLine(`Creating version plan file "${versionPlanFileName}"`);
            (0, print_changes_1.printDiff)('', versionPlanFileContent, 1);
            const versionPlansAbsolutePath = (0, version_plans_1.getVersionPlansAbsolutePath)();
            await (0, fs_extra_1.ensureDir)(versionPlansAbsolutePath);
            await (0, fs_extra_1.writeFile)((0, node_path_1.join)(versionPlansAbsolutePath, versionPlanFileName), versionPlanFileContent);
        }
        return 0;
    };
}
async function promptForVersion(message) {
    try {
        const reply = await (0, enquirer_1.prompt)([
            {
                name: 'version',
                message,
                type: 'select',
                choices: [...semver_1.RELEASE_TYPES, 'none'],
            },
        ]);
        return reply.version;
    }
    catch (e) {
        output_1.output.log({
            title: 'Cancelled version plan creation.',
        });
        process.exit(0);
    }
}
async function promptForMessage(versionPlanName) {
    let message;
    do {
        message = await _promptForMessage(versionPlanName);
    } while (!message);
    return message;
}
async function _promptForMessage(versionPlanName) {
    try {
        const reply = await (0, enquirer_1.prompt)([
            {
                name: 'message',
                message: 'What changelog message would you like associated with this change? (Leave blank to open an external editor for multi-line messages/easier editing)',
                type: 'input',
            },
        ]);
        let message = reply.message.trim();
        if (!message.length) {
            const tmpDir = (0, tmp_1.dirSync)().name;
            const messageFilePath = (0, node_path_1.join)(tmpDir, `DRAFT_MESSAGE__${versionPlanName}.md`);
            (0, fs_extra_1.writeFileSync)(messageFilePath, '');
            await (0, launch_editor_1.launchEditor)(messageFilePath);
            message = (0, fs_extra_1.readFileSync)(messageFilePath, 'utf-8');
        }
        message = message.trim();
        if (!message) {
            output_1.output.warn({
                title: 'A changelog message is required in order to create the version plan file',
                bodyLines: [],
            });
        }
        return message;
    }
    catch (e) {
        output_1.output.log({
            title: 'Cancelled version plan creation.',
        });
        process.exit(0);
    }
}
