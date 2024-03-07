const { Given, When, Then } = require('@cucumber/cucumber');
const api = require('../../libs/api.js')
const {createStructure, getStructureInfo, linkUserToStructure} = require('../../libs/structure-api.js')
const {login, logout, admcLogin, createAndActivateUser, getUserFromTestAlias, getUserInfo, setAdmlOnStructures} = require('../../libs/user-api.js')

Given('A structure {string}', async  function (structureName) {
   await admcLogin(this)
   await createStructure(structureName, this)
 });

 Given('A user {string} on the structure {string}', async function (userTestAlias, structureName) {
   this.admlUser = userTestAlias
   const structureId = getStructureInfo(structureName, this).id
   try {
    const userInfos = await getUserInfo(userTestAlias, this)
    const userStructures = (userInfos.structures || []).map(s => s.externalId)
    if(userStructures.indexOf(structureId) >= 0) {
        console.debug(`${userTestAlias} already on ${structureName}`)
    } else {
        await linkUserToStructure(userInfos.externalId, structureId, this)
    }
   } catch(e) {
    await createAndActivateUser(userTestAlias, {
        firstName: `User ${userTestAlias} on ${structureName} FN`,
        lastName: `User ${userTestAlias} on ${structureName} LN`,
        type: 'Teacher',
        structureId
    }, this)
   }
 });

 Given('User {string} is ADML on the structure {string}', async function (userTestAlias, structureName) {
   const structureId = getStructureInfo(structureName, this).id
   await setAdmlOnStructures(userTestAlias, [structureId], this)
 });

 When('{string} adds the ADML function to {string} on structure {string}', async function (adml, user, structureName) {
   const structureId = getStructureInfo(structureName, this).id
   if(adml === 'ADMC') {
    await admcLogin(this)
   } else {
       const admlId = getUserFromTestAlias(adml, this).login
       await login(admlId, null, this)
   }
   this.setAdmlResponse = await setAdmlOnStructures(user, [structureId], this)
 });

 Then('I get a {int} response', function (expectedCode) {
    if(this.setAdmlResponse.status !== expectedCode) {
        throw `Expected the response to give back a ${expectedCode} code but got ${this.setAdmlResponse.status}`
    }
 });
