describe('template spec', function () {

  this.beforeEach(() => {
    cy.visit(Cypress.env('url') + '/resource/Start');
    cy.login(Cypress.env('authType'), { user: Cypress.env('user'), pass: Cypress.env('password') });
  })

  it('Empty test shell', () => {

  })

})