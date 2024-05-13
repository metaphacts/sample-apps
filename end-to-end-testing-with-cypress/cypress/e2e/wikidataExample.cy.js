describe('template spec', function () {

  this.beforeEach(() => {
    cy.visit('https://wikidata.metaphacts.com/resource/app:Start');
  })

  it('Verify navigation to Albert Einstein (Entity Q937)', () => {
    cy.get('#react-select-2-input').type('einstein');
    cy.get('#react-select-2-option-0 > span').click();
    cy.get('.knowledgeGraphBar__uriRow > a > :nth-child(1)').should('have.text', 'http://www.wikidata.org/entity/Q937');
  })

})