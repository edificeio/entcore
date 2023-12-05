const axios = require('axios');
const {ROOT_URL, ADMC_EMAIL, ADMC_PASSWORD, DEFAULT_PASSWORD} = require('./env.js')

/**
 * @param {*} world The current scenario object
 * @returns information about the currently logged in user or null
 */
function getCurrentOneSessionId(world) {
    if(world.sessions) {
        return world.sessions[world.sessions['current']]
    }
    return null;
}
/**
 * Execute a GET request.
 * Note that the response is not necessarily OK as the status is not validated.
 * @param {string} uri Path of the url
 * @param {*} world The current scenario object
 * @param {boolean} unauthenticated true if the call has to be anonymous, false otherwise
 * @returns raw axios's response 
 */
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
/**
 * POST a json body to the specified URI.
 * Note that the response is not necessarily OK as the status is not validated.
 * @param {string} uri Path of the url
 * @param {*} data The body content of the request as a JSON object
 * @param {*} world The current scenario object
 * @param {boolean} unauthenticated true if the call has to be anonymous, false otherwise
 * @returns raw axios's response 
 */
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
/**
 * POST a form encoded body to the specified URI.
 * Note that the response is not necessarily OK as the status is not validated.
 * @param {string} uri Path of the url
 * @param {*} data The body content of the request as a JSON object
 * @param {*} world The current scenario object
 * @param {boolean} unauthenticated true if the call has to be anonymous, false otherwise
 * @returns raw axios's response 
 */
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

/**
 * @param {string} cookieName Name of the cookie to extract
 * @param {axios.Response} axiosResponse Raw response from which to extract the cookie value
 * @returns Value of the cookie or null if none was found
 */
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