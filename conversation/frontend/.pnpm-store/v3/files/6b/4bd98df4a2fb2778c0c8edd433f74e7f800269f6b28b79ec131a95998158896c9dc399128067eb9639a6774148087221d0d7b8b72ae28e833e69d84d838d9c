"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const fs_1 = require("fs");
const git_utils_1 = require("./git-utils");
// This script is used as an editor for git rebase -i
// This is the file which git creates. When this script exits, the updates should be written to this file.
const filePath = process.argv[2];
// Change the second commit from pick to fixup
const contents = (0, fs_1.readFileSync)(filePath).toString();
const newContents = (0, git_utils_1.updateRebaseFile)(contents);
// Write the updated contents back to the file
(0, fs_1.writeFileSync)(filePath, newContents);
