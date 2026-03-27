import { IAttributes, IController, IDirective, IScope } from "angular";
import { notify } from "entcore";
import { http } from "ode-ngjs-front";

type UserPrefs = { homePage: { betaEnabled: boolean } | null };

export class Controller implements IController {
  public isSwitching = false;
  public isBetaActivated: boolean = false;

  async activateHomepage() {
    if (this.isSwitching) return;

    this.isSwitching = true;
    try {
      await http().putJson("/userbook/api/preferences", {
        homePage: { betaEnabled: true },
      } as UserPrefs);
      location.reload();
    } catch {
      notify.error("Erreur de traitement ou reseau.");
      this.isSwitching = false;
    }
  }
}

class Directive implements IDirective<
  IScope,
  JQLite,
  IAttributes,
  IController[]
> {
  restrict = "E";
  replace = true;
  template = require("./beta-switch.directive.html");
  scope = {};
  bindToController = true;
  controller = [Controller];
  controllerAs = "ctrl";
  require = ["betaSwitch"];

  link(
    scope: IScope,
    elem: JQLite,
    attr: IAttributes,
    controllers: IController[] | undefined,
  ) {
    let ctrl: Controller | null = controllers
      ? (controllers[0] as Controller)
      : null;
    if (!ctrl) return;

    http()
      .get("/userbook/api/preferences")
      .then((userPrefs: UserPrefs) =>
        !userPrefs.homePage ? false : userPrefs?.homePage.betaEnabled,
      )
      .then((value) => {
        if (value) {
          ctrl.isBetaActivated = value;
          scope.$apply();
        }
      });
  }
}

export function DirectiveFactory() {
  return new Directive();
}
