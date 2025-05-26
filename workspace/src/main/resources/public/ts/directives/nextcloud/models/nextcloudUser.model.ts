export interface IUserResponse {
  id: string;
  displayname: number;
  email: string;
  phone: string;
  itemsperpage: string;
  quota: {
    free: string;
    used: string;
    total: string;
    relative: number;
    quota: number;
  };
}

export class UserNextcloud {
  id: string;
  displayName: number;
  email: string;
  phone: string;
  itemsPerPage: string;
  quota: Quota;

  build(data: IUserResponse): UserNextcloud {
    this.id = data.id;
    this.displayName = data.displayname;
    this.email = data.email;
    this.phone = data.phone;
    this.itemsPerPage = data.itemsperpage;
    this.quota = new Quota().build(data.quota);
    return this;
  }
}

export class Quota {
  used: number;
  total: number;
  unit: string;

  build(data: any): Quota {
    this.used = data.used / (1024 * 1024);
    this.total = data.quota / (1024 * 1024);
    if (this.total > 2000) {
      this.total = Math.round((this.total / 1024) * 100) / 100;
      this.used = Math.round((this.used / 1024) * 100) / 100;
      this.unit = "Go";
    } else {
      this.total = Math.round(this.total);
      this.used = Math.round(this.used);
      this.unit = "Mo";
    }
    return this;
  }
}
