const axios = require('axios');
const {post, get} = require('./api.js')

/**
 * 
 * @param {string} name Name of the structure to create
 * @param {*} world The current scenario object
 * @returns Information about the newly created structure
 */
async function createStructure(name, world) {
    const structure = {
        name,
        hasApp: true,
    }
    const response = await post('/directory/school', structure, world)
    if(response.status === 201) {
        const structInfo = response.data
        world.structures = world.structures || {}
        world.structures[name] = {...structure, ...structInfo}
        return response.data
    } else {
        console.error(response)
        throw "school.cannot.be.created"
    }
}
/**
 * 
 * @param {string} name Name of the structure as it was defined in this scenario 
 * (i.e. it will look for a structure previously created IN THIS SCENARIO with the supplied name)
 * @param {*} world The current scenario object
 * @returns structure's information if they could be fetched successfully, otherwise it raises an error
 */
async function fetchStructureUsers(name, world) {
    const structureId = getStructureInfo(name, world).id
    const response = await get('/directory/structure/${structureId}/users', world)
    if(response.status === 200) {
        return response.data
    } else {
        console.error(response)
        throw "structure.users.cannot.be.fetched"
    }
}
/**
 * 
 * @param {string} userId externalId of the user to add
 * @param {string} structureId Id of the structure to which the user should be added
 * @param {*} world The current scenario object
 * @returns true if the user could be added, raises an error otherwise
 */
async function linkUserToStructure(userId, structureId, world) {
    const response = await post(`/structure/${structureId}/link/${userId}`, {}, world)
    if(response.status === 200) {
        return true
    } else {
        console.error(response)
        throw "link.cannot.be.created"
    }
}
function getStructureInfo(name, world) {
    world.structures = world.structures || {}
    return world.structures[name]
}
exports.createStructure = createStructure
exports.getStructureInfo = getStructureInfo
exports.linkUserToStructure = linkUserToStructure
exports.fetchStructureUsers = fetchStructureUsers