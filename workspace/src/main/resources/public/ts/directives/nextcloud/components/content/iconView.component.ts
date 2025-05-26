interface INextcloudViewIcons {}

export class NextcloudViewIcons implements INextcloudViewIcons {
  private vm: any;
  private scope: any;

  constructor(scope) {
    this.scope = scope;
    this.vm = scope.vm;
  }
}
