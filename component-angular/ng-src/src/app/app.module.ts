import { BrowserModule } from '@angular/platform-browser';
import { NgModule, Injector } from '@angular/core';
import {createCustomElement} from '@angular/elements';

import { HelloWorldComponent } from './hello-world/hello-world.component';

@NgModule({
  declarations: [
    HelloWorldComponent
  ],
  imports: [
    BrowserModule
  ],
  providers: [],
  entryComponents: [HelloWorldComponent]
})
export class AppModule {

  constructor(private injector: Injector) {}

  ngDoBootstrap() {
    const HelloWorldElement = createCustomElement(HelloWorldComponent,
                                 { injector: this.injector });
    customElements.define('hello-world-angular', HelloWorldElement);
  }
}
