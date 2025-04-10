"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.isWorkspaceClaimed = isWorkspaceClaimed;
const get_cloud_options_1 = require("./get-cloud-options");
async function isWorkspaceClaimed(nxCloudAccessToken) {
    if (!nxCloudAccessToken)
        return false;
    const apiUrl = (0, get_cloud_options_1.getCloudUrl)();
    try {
        const response = await require('axios').post(`${apiUrl}/nx-cloud/is-workspace-claimed`, {
            nxCloudAccessToken,
        });
        if (response.data.message) {
            return false;
        }
        else {
            return response.data;
        }
    }
    catch (e) {
        // We want to handle cases the if the request fails for any reason
        return false;
    }
}
