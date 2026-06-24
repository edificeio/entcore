export interface GoogleDriveCreateDocumentType {
  label: string;
  icon: string;
  type: string;
}

// Type keys must match GoogleDriveConfig.CREATE_DOCUMENT_TARGET_URLS (io.edifice.google.drive), which resolves the actual create URLs.
export const GOOGLE_DRIVE_CREATE_DOCUMENT_TYPES: GoogleDriveCreateDocumentType[] = [
  { label: "google-drive.create.document", icon: "/workspace/public/img/google-drive/create-docs.svg", type: "document" },
  { label: "google-drive.create.spreadsheet", icon: "/workspace/public/img/google-drive/create-sheets.svg", type: "spreadsheet" },
  { label: "google-drive.create.presentation", icon: "/workspace/public/img/google-drive/create-slides.svg", type: "presentation" },
  { label: "google-drive.create.form", icon: "/workspace/public/img/google-drive/create-forms.svg", type: "form" },
  { label: "google-drive.create.video", icon: "/workspace/public/img/google-drive/create-vids.svg", type: "video" },
];
