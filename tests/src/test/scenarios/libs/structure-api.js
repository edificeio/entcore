const axios = require('axios');
const {post, get} = require('./api.js')

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