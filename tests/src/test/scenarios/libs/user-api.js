const {get, post, postForm, getCurrentOneSessionId, getCookieByName} = require('./api.js')
const {ADMC_EMAIL, ADMC_PASSWORD, DEFAULT_PASSWORD} = require('./env.js')

/**
 * Return user's information fetched by a call to /auth/oauth2/userinfo
 * @param {string} idt Login of the user
 * @param {*} world The current scenario object
 * @returns User's detailed information
 */
async function getUserInfo(idt, world) {
    const oldSession = getCurrentOneSessionId(world)
    try {
        await login(idt, DEFAULT_PASSWORD, world)
        return await get('/auth/oauth2/userinfo', world)
    } finally {
        await login(oldSession, '', world)
    }
}

/**
 * 
 * @param {string} id Login of the user
 * @param {string} password Password of the user or null if we want to use the value of the default password
 * @param {*} world The current scenario object
 * @returns the logged in user's sessions
 */
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
/**
 * Logout the currently connected user
 * @param {*} world The current scenario object
 */
async function logout(world) {
    delete world.sessions.current;
}
/**
 * Logs the ADMC.
 * @param {*} world The current scenario object
 * @returns ADMC's information
 */
async function admcLogin(world) {
    const data = await login(ADMC_EMAIL, ADMC_PASSWORD, world)
    world.users = world.users || {}
    if(!world.users.ADMC) {
        world.users.ADMC = (await get('/auth/oauth2/userinfo', world)).data
    }
    return data
}
/**
 * 
 * @param {string} idt User login
 * @param {*} world The current scenario object
 * @returns true if the user exists and false otherwose
 */
async function userExists(idt, world) {
    const oldSession = getCurrentOneSessionId(world)
    try {
        await logout(world)
        const loginResponse = await login(idt, DEFAULT_PASSWORD, world)
        return loginResponse.status === 302 && !!getCookieByName('oneSessionId', loginResponse)
    } catch(e) {
        return false
    } finally {
        await login(oldSession, '', world)
    }
}
/**
 * Create a user and add their information in the state of the scenario.
 * @param {string} testAlias The alias of the user in the scenario
 * @param {*} userInfo Information to create the user
 * @param {*} world The current scenario object
 * @returns Created user information or raise "user.cannot.be.created"
 */
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

/**
 * Create a user, activate them and add their information in the state of the scenario.
 * @param {string} testAlias The alias of the user in the scenario
 * @param {*} userInfo Information to create the user
 * @param {*} world The current scenario object
 * @returns Created user information or raise "user.cannot.be.created"
 */
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
        phone: '0601010101'
    }
    try {
        await postForm('/auth/activation', request, true, world)
    } finally {
        await admcLogin(world)
    }

}
/**
 * 
 * @param {string} testAlias The way the user is called in the scenario (e.g. "user1", "userAdml")
 * @param {*} world The current scenario object
 * @returns Detailed information about the user (login, id, etc.)
 */
function getUserFromTestAlias(testAlias, world) {
    world.users = world.users || {}
    return world.users[testAlias]
}

/**
 * Tries to replace the current user's functions scope by the supplied structures
 * @param {string} userTestAlias The way the user is called in the scenario (e.g. "user1", "userAdml")
 * @param {*} structureIds Ids of the structure who should be administered by the user
 * @param {*} world The current scenario object
 * @returns 
 */
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