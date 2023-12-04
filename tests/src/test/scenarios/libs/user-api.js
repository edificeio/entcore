const {get, post, postForm, getCurrentOneSessionId, getCookieByName} = require('./api.js')
const {ADMC_EMAIL, ADMC_PASSWORD, DEFAULT_PASSWORD} = require('./env.js')

async function getUserInfo(idt, world) {
    const oldSession = getCurrentOneSessionId(world)
    try {
        await login(idt, DEFAULT_PASSWORD, world)
        return await get('/auth/oauth2/userinfo', world)
    } finally {
        await login(oldSession, '', world)
    }
}


async function login(id, password, world) {
    const passwordToUSe = password || DEFAULT_PASSWORD
    world.sessions = world.sessions || {}
    if(!world.sessions[id]) {
        const loginResponse = await postForm('/auth/login', {email: id, password: passwordToUSe}, true, world)
        if(loginResponse.status === 302) {
            const oneSessionId = getCookieByName('oneSessionId', loginResponse)
            world.sessions[id] = oneSessionId
        } else {
            throw "cannot.login.as." + id
        }
    }
    world.sessions['current'] = id
    return world.sessions[id]
}
async function logout(world) {
    delete world.sessions.current;
}

async function admcLogin(world) {
    const data = await login(ADMC_EMAIL, ADMC_PASSWORD, world)
    world.users = world.users || {}
    if(!world.users.ADMC) {
        world.users.ADMC = (await get('/auth/oauth2/userinfo', world)).data
    }
    return data
}

async function userExists(idt, world) {
    const oldSession = getCurrentOneSessionId(world)
    try {
        await login(idt, DEFAULT_PASSWORD, world)
        return true
    } finally {
        await login(oldSession, '', world)
    }
    return false
}

async function createUser(testAlias, userInfo, world) {
    const response = await postForm(`/directory/api/user`, userInfo, null, world)
    if(response.status === 200) {
        const data = response.data
        world.users = world.users || {}
        world.users[testAlias] = data
        return data
    } else {
        console.error(response)
        throw "user.cannot.be.created"
    }
}

async function createAndActivateUser(testAlias, userInfo, world) {
    const {id, login} = await createUser(testAlias, userInfo, world)
    const activateResponse = await get(`/directory/user/${id}`, world)
    await logout(world)
    const activationCode = activateResponse.data.activationCode
    const request = {
        theme: 'cg771d',
        login,
        password: DEFAULT_PASSWORD,
        confirmPassword: DEFAULT_PASSWORD,
        acceptCGU: 'true',
        activationCode,
        callBack: '',
        mail: `junior.bernard+${testAlias}@edifice.io`,
        phone: '0632421927'
    }
    try {
        await postForm('/auth/activation', request, true, world)
    } catch(e) {
        console.error(e)
    } finally {
        await admcLogin(world)
    }

}

function getUserFromTestAlias(testAlias, world) {
    world.users = world.users || {}
    return world.users[testAlias]
}

async function setAdmlOnStructures(userTestAlias, structureIds, world) {
    world.users = world.users || {}
    const user = world.users[userTestAlias]
    const userId = user.id || user.externalId
    const data = {
        functionCode: "ADMIN_LOCAL",
        inherit: "s",
        scope: structureIds
    }
    return await post(`/directory/user/function/${userId}`, data, world)
}

exports.getUserInfo = getUserInfo
exports.userExists = userExists
exports.createUser = createUser
exports.createAndActivateUser = createAndActivateUser
exports.setAdmlOnStructures = setAdmlOnStructures
exports.getUserFromTestAlias = getUserFromTestAlias
exports.login = login;
exports.logout = logout;
exports.admcLogin = admcLogin;