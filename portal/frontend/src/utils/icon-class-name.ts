import { Application } from "~/models/application";

export const getIconClass = (app:Application) => {
  const appCode:string = getIconCode(app);
  return `ic-app-${appCode} color-app-${appCode}`;
}

export const getIconCode = (app:Application) => {
  let appCode = app.icon.trim().toLowerCase() || "";
  if( appCode && appCode.length > 0 ) {
    if(appCode.endsWith("-large"))  appCode = appCode.replace("-large", "");
  } else {
    appCode = app.displayName.trim().toLowerCase();
  }
  appCode = appCode.normalize("NFD").replace(/[\u0300-\u036f]/g, "");
  switch( appCode ) {
    case "admin.title":
      appCode = "admin";
      break;
    case "banques des savoirs":
      appCode = "banquesavoir";
      break;
    case "collaborativewall":
      appCode = "collaborative-wall";
      break;
    case "communautés":
      appCode = "community";
      break;
    case "directory.user":
      appCode = "userbook";
      break;
    case "emploi du temps":
      appCode = "edt";
      break;
    case "messagerie":
      appCode = "conversation";
      break;
    case "news":
      appCode = "actualites";
      break;
    case "homeworks":
    case "cahier de texte":
      appCode = "cahier-de-texte";
      break;
    case "diary":
    case "cahier de texte 2d":
      appCode = "cahier-textes";
      break;
    default: break;
	}
	return appCode;
}