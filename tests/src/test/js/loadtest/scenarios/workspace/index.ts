import { UserSetupData } from "../node_modules/edifice-k6-commons/dist/index.js";
import { group } from "k6";

export function workspace(_: UserSetupData) {
  group("[workspace]", () => {});
}
