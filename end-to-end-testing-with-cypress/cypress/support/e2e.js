
// Import commands.js using ES2015 syntax:
import './register.cmd'
import '../commands/cypress.commands';
import './auth';

// ignore all console errors in app
// eslint-disable-next-line no-unused-vars
Cypress.on('uncaught:exception', (err, runnable) => false)