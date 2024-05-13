//custom function to determine the selector used when running studio
//this is currently needed to address react-bootstrap component markup
//ideally we would use data-testid everywhere, but some of the bootstrap components
//dont want to pass that 'data-' attribute through with the 'data-' part still appended
Cypress.SelectorPlayground.defaults({
    onElement: ($el) => {

        //workaround for react select input selector
        //we should instead be passing a custom input component to React Select when we want
        //to interact with it via cypress, but this has issues with the current props we pass
        //to some of the react-select components (DropdownSuggestion)
        const inputId = $el.attr('id')
        if(inputId && inputId.startsWith('react-select-input')){
            return `[id='${inputId}']`;
        }

        const dataTestid = $el.attr('data-testid');
        if (dataTestid) {
            return `[data-testid='${dataTestid}']`
        }

        const testid = $el.attr('testid');
        if(testid){
            return `[testid='${testid}']`;
        }
        //check parent for data-testid
        //This is necessary due to the markup of some react-bootstrap components
        //the tool wanted to use the child selector even if the parent had a data-testid,
        //or testid attribute, so this addresses that issue
        if ($el.offsetParent()) {
            const parentCustomId = $el.offsetParent().attr('data-testid');
            if (parentCustomId) {
                return (`[data-testid=${parentCustomId}]`);
            }
            const parentCustomDataId = $el.offsetParent().attr('testid');
            if(parentCustomDataId){
                return `[testid=${parentCustomDataId}]`;
            }
        }
        return $el;
    },
})