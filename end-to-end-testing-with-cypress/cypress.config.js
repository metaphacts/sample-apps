/* eslint-disable global-require */
// eslint-disable-next-line import/no-extraneous-dependencies
const { defineConfig } = require('cypress')

module.exports = defineConfig({
    numTestsKeptInMemory: 15,
    defaultCommandTimeout: 15000,
    env: {
        //replace this with your own metaphacts url
        url: 'http://localhost:10214',
        //if using form auth, user and password should be filled out
        user: 'admin',
        password: 'admin',
        //'form' or 'session'
        authType: 'form',
        // sessionId: ''
    },
    video: true,
    retries: {
        runMode: 1,
        openMode: 0
    },
    userAgent:
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.190 Safari/537.36',
    viewportHeight: 1080,
    viewportWidth: 1920,
    e2e: {
        experimentalStudio: true,
    },
})
