const axios = require('axios');
const {ROOT_URL, ADMC_EMAIL, ADMC_PASSWORD, DEFAULT_PASSWORD} = require('./env.js')

function getCurrentOneSessionId(world) {
    if(world.sessions) {
        return world.sessions[world.sessions['current']]
    }
    return null;
}

async function get(uri, world, unauthenticated) {
    const headers = { }
    if(!unauthenticated) {
        headers['Cookie'] = `oneSessionId=${getCurrentOneSessionId(world)}`
    }
    return await axios({
        method: 'GET',
        url: `${ROOT_URL}${uri}`,
        headers,
        maxRedirects: 0,
        validateStatus: false
    })
}

async function post(uri, data, world, unauthenticated) {
    const headers = { "Content-Type": "application/json" }
    if(!unauthenticated) {
        headers['Cookie'] = `oneSessionId=${getCurrentOneSessionId(world)}`
    }
    return await axios({
        method: 'POST',
        url: `${ROOT_URL}${uri}`,
        data,
        headers,
        maxRedirects: 0,
        validateStatus: false
    })
}
async function postForm(uri, data, unauthenticated, world) {
    const headers = { "Content-Type": "application/x-www-form-urlencoded" }
    if(!unauthenticated) {
        headers['Cookie'] = `oneSessionId=${getCurrentOneSessionId(world)}`
    }
    const bodyFormData = new FormData();
    Object.keys(data).forEach(key => bodyFormData.append(key, data[key]))
    return await axios({
        method: "post",
        url: `${ROOT_URL}${uri}`,
        data: bodyFormData,
        maxRedirects: 0,
        validateStatus: false,
        headers,
    })
}

function getCookieByName(cookieName, axiosResponse) {
    const cookie = axiosResponse.headers['set-cookie']
    .find(cookie => cookie.includes(cookieName))
    ?.match(new RegExp(`^${cookieName}=(.+?);`))
    ?.[1];
    return cookie;
}

exports.get = get;
exports.post = post;
exports.postForm = postForm;
exports.getCurrentOneSessionId = getCurrentOneSessionId;
exports.getCookieByName = getCookieByName;