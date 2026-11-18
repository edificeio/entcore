/**
 * CSV files of the import wizard (POST /directory/wizard/import), as a person of an academy or a school would send
 * them. Pure : no k6, no http. The columns are the ones the feeder recognises (see ProfileColumnsMapper) :
 * the people are identified by an `externalId` column, and the children of a relative by `childExternalId` columns
 * (one per child), so that a relative can be attached to students imported by another source (AAF, AAF1D)
 * without having to repeat their names, their title and their class.
 */

export type CsvRelative = {
  /** Given to the account, whatever the source of the students. */
  externalId: string;
  firstName: string;
  lastName: string;
  /** externalId of the children, as they are on the platform (with the prefix of the import directory, if any). */
  childExternalIds: string[];
};

function quote(value: string): string {
  return `"${value.replace(/"/g, '""')}"`;
}

/** The file of the relatives : "externalId";"Nom responsable";"Prénom responsable";"childExternalId" (repeated). */
export function relativesCsv(relatives: CsvRelative[]): string {
  const nbChildren = Math.max(1, ...relatives.map((r) => r.childExternalIds.length));
  const header = ["externalId", "Nom responsable", "Prénom responsable"];
  for (let i = 0; i < nbChildren; i++) header.push("childExternalId");
  const lines = [header.map(quote).join(";")];
  relatives.forEach((relative) => {
    const cells = [relative.externalId, relative.lastName, relative.firstName];
    for (let i = 0; i < nbChildren; i++) cells.push(relative.childExternalIds[i] || "");
    lines.push(cells.map(quote).join(";"));
  });
  return lines.join("\n") + "\n";
}
