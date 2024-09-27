describe('template spec', function () {

  this.beforeEach(() => {
    cy.visit(Cypress.env('url') + '/resource/Start');
    cy.login();
  })

  it('Empty test shell', () => {

  })

})