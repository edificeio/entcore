export enum DocumentRole {
  XLS = "spreadsheet",
  PPT = "presentation",
  VIDEO = "video",
  // Matches the theme's own icon class convention (e.g. "icons/img.png"), same as "pdf"/"doc"
  // below — anything else here would neither pick up the theme's icon nor match it visually.
  IMG = "img",
  AUDIO = "audio",
  DOC = "doc",
  PDF = "pdf",
  UNKNOWN = "unknown",
  FOLDER = "folder",
}
