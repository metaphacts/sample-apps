//SESSION AUTH
export const useSessionAuth = (sessionId) => {
    cy.setCookie('JSESSIONID', sessionId)
}
//END SESSION AUTH

//FORM AUTH
export const useFormAuth = (user, pass) => {
    cy.get('[data-testid="input-username"]').type(user);
    cy.get('[data-testid="input-password"]').type(pass + '{enter}');
}
//END FORM AUTH

//Generic login command, handles session or form auth currently
Cypress.Commands.add('login', () => {
    const type = Cypress.env('authType');
    const user = Cypress.env('user');
    const pass = Cypress.env('password');
    const sessionId = Cypress.env('sessionId');
    if (type === 'form') {
        useFormAuth(user, pass);
    }
    if (type === 'session') {
        useSessionAuth(sessionId);
    }
});