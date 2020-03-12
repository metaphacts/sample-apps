class HelloWorldFromApp extends HTMLElement {
    constructor() {
      super();
      this._shadowRoot = this.attachShadow({mode: 'open'});
    }
    static get observedAttributes(){
      return ["name"];
    }
    attributeChangedCallback(name, oldValue, newValue) {
      this._shadowRoot.innerHTML = `<p>hello ${newValue}</p>`;
    }
    connectedCallback(){
      let name = this.getAttribute("name");
      this._shadowRoot.innerHTML = `<p>Hello ${name}</p>`;
    }
  }

customElements.define('hello-from-app', HelloWorldFromApp);
