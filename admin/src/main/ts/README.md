# Admin

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 8.3.19.

## Development with proxy server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

To connect to a remote server instead of your local springboard, create a `.env` file in this directory with your credentials:

**Option 1 — automatic (recommended):** use [dev-auth-fetcher](https://github.com/edificeio/dev-auth-fetcher) which handles authentication and generates the `.env` automatically.

**Option 2 — manual:** copy `.env.template` to `.env` and fill in `VITE_RECETTE`, `VITE_XSRF_TOKEN` and `VITE_ONE_SESSION_ID`, then run `ng serve`.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `--prod` flag for a production build.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).
