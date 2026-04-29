import { IAttributes, IController, IDirective, IScope } from "angular";
import { notify } from "entcore";
import { http, session } from "ode-ngjs-front";

type UserPrefs = {
  homePage: { closeBetaSwitch?: string; betaEnabled?: boolean } | null;
};

const DEFAULT_BETA_HIDE_DURATION_DAYS = 7;

export class Controller implements IController {
  public isSwitching = false;
  public isBetaVisible: boolean = false;

  async activateHomepage() {
    if (this.isSwitching) return;

    this.isSwitching = true;
    try {
      await http().putJson("/userbook/api/preferences", {
        homePage: { betaEnabled: true },
      } as UserPrefs);
      location.reload();
    } catch {
      notify.error("timeline.beta.switch.error");
      this.isSwitching = false;
    }
  }

  async hideBetaMessage() {
    if (this.isSwitching) return;

    this.isBetaVisible = false;
      this.isSwitching = true;
    try {
      await http().putJson("/userbook/api/preferences", {
        homePage: { closeBetaSwitch: new Date().toISOString() },
      } as UserPrefs);
    } catch {
      notify.error("timeline.beta.hide.error");
      this.isBetaVisible = true;
    } finally {
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
      .then((userPrefs: UserPrefs) => {
        const betaEnabled = session().hasWorkflow(
          "org.entcore.timeline|betaActivation",
        );
        let userCloseBetaDate = userPrefs?.homePage?.closeBetaSwitch
          ? new Date(userPrefs.homePage.closeBetaSwitch)
          : null;
        
        if (userCloseBetaDate && !isNaN(userCloseBetaDate?.getTime())) {
            userCloseBetaDate.setDate(
              userCloseBetaDate.getDate() + DEFAULT_BETA_HIDE_DURATION_DAYS,
            ); // Add x days to the closeBetaSwitch date
        } else {
          // If the date is invalid, consider the beta message as not closed
          userCloseBetaDate = null;
        }
        const displayUserBeta =
          !userCloseBetaDate || new Date() < userCloseBetaDate;

        ctrl.isBetaVisible = betaEnabled && displayUserBeta;
        scope.$apply();
      });
  }
}

export function DirectiveFactory() {
  return new Directive();
}
