const fs = require('fs-extra');
const concat = require('concat');
(async function build() {
  const files = [
    './dist/hello-world-angular/runtime.js',
    './dist/hello-world-angular/polyfills.js',
    './dist/hello-world-angular/scripts.js',
    './dist/hello-world-angular/main.js',
  ]
  await fs.ensureDir('target')
  await concat(files, 'target/hello-world-angular.js');
  await fs.copyFile('./dist/hello-world-angular/styles.css', 'target/styles-angular.css')
})()
