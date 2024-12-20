//SESSION AUTH
export const useSessionAuth = () => {
    const sessionId = Cypress.env('sessionId');
    cy.setCookie('JSESSIONID', sessionId)
}
//END SESSION AUTH

//FORM AUTH
export const useFormAuth = () => {
    const user = Cypress.env('user');
    const pass = Cypress.env('password');
    cy.visit(Cypress.env('url') + '/login');
    cy.get('[data-testid="input-username"]').type(user);
    cy.get('[data-testid="input-password"]').type(pass + '{enter}');
}
//END FORM AUTH

//TOKEN AUTH
export const useTokenAuth = () => {
    console.log("using token authentication");
    // apply bearer token, see command 'useTokenAuth in cypress.commands.js'
    cy.useTokenAuth();
}
//END TOKEN AUTH

//Generic login command, handles session or form auth currently
Cypress.Commands.add('login', () => {
    const type = Cypress.env('authType');
    if (type === 'form') {
        useFormAuth();
    }
    if (type === 'session') {
        useSessionAuth();
    }
    if(type === 'authToken') {
        useTokenAuth();
    }
})