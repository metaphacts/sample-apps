//SESSION AUTH
export const useSessionAuth = (sessionId) => {
    cy.setCookie('JSESSIONID', SESSIONID)
}
//END SESSION AUTH

//FORM AUTH
export const useFormAuth = (user, pass) => {
    cy.get('[data-testid="input-username"]').type(user);
    cy.get('[data-testid="input-password"]').type(pass + '{enter}');
}
//END FORM AUTH

//Generic login command, handles session or form auth currently
Cypress.Commands.add('login', (type, params) => {
    if (type === 'form') {
        useFormAuth(params.user, params.pass);
    }
    if (type === 'session') {
        useSessionAuth(params.sessionId)
    }
})