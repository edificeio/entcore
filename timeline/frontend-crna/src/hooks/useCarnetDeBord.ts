import { useMemo } from "react";
import { useEdificeClient } from "@edifice.io/react";
import { useQuery } from "@tanstack/react-query";
import { carnetDeBordQueryOptions } from "~/services/queries/carnetDeBord.queries";
import type {
  ContentItem,
  ContentTitle,
  ContentType,
  ParsedEleve,
  Structure,
  UseCarnetDeBordResult,
} from "~/models/carnetDeBord";

export type { ContentItem, ContentTitle, ContentType, ParsedEleve, UseCarnetDeBordResult };

const INITIAL_CONTENT_TYPES: ContentType[] = [
  { title: "lateness", compact: false, full: false, lightboxTitle: "carnet-de-bord.widget.lateness.all" },
  { title: "absences", compact: false, full: false, lightboxTitle: "carnet-de-bord.widget.absences.all" },
  { title: "grades",   compact: false, full: false, lightboxTitle: "carnet-de-bord.widget.grades.all" },
  { title: "diary",    compact: false, full: false, lightboxTitle: "carnet-de-bord.widget.diary.all" },
  { title: "skills",   compact: false, full: false, lightboxTitle: "carnet-de-bord.widget.skills.all" },
];

const getText = (parent: Element, selector: string): string =>
  parent.querySelector(selector)?.textContent?.trim() ?? "";

const formatDate = (value: string, withTime = false): string => {
  if (!value) return "";
  const date = new Date(value);
  return new Intl.DateTimeFormat("fr-FR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    ...(withTime && { hour: "2-digit", minute: "2-digit" }),
  }).format(date);
};

type UserChild = { firstName: string; lastName: string };

const parseEleves = (
  structures: Structure[],
  userChildren: Record<string, UserChild>
): ParsedEleve[] => {
  const parser = new DOMParser();
  return structures.flatMap((structure) => {
    const doc = parser.parseFromString(structure.xmlResponse, "text/xml");
    return Array.from(doc.querySelectorAll("Eleve")).map((element) => {
      const prenom = element.querySelector("Prenom")?.textContent?.trim() ?? "";
      const nom = element.querySelector("Nom")?.textContent?.trim() ?? "";
      // Match child by name, same logic as AngularJS getChildId()
      const matchedId = Object.entries(userChildren).find(
        ([, child]) =>
          child.firstName.toLowerCase() === prenom.toLowerCase() &&
          child.lastName.toLowerCase() === nom.toLowerCase()
      )?.[0];
      const avatar = matchedId ? `/userbook/avatar/${matchedId}?thumbnail=100x100` : "";
      return { element, name: prenom, avatar, address: structure.address };
    });
  });
};

const computeForEleve = (contentType: ContentType, eleve: Element): ContentType => {
  const next: ContentType = { ...contentType, compact: false, full: false };

  if (contentType.title === "lateness") {
    const items: ContentItem[] = [];
    eleve.querySelectorAll("Retard").forEach((el) => {
      if (getText(el, "Justifie") === "false") {
        items.push({ value: `le ${formatDate(getText(el, "Date"), true)}`, pageUrl: el.getAttribute("page") ?? undefined });
      }
    });
    next.compact = items[0]?.value ?? false;
    next.full = items;
  }

  if (contentType.title === "absences") {
    const items: ContentItem[] = [];
    eleve.querySelectorAll("Absence").forEach((el) => {
      if (getText(el, "Justifie") === "false") {
        const isOpened = getText(el, "EstOuverte") === "true";
        const value = isOpened
          ? `le ${formatDate(getText(el, "DateDebut"), true)}`
          : `du ${formatDate(getText(el, "DateDebut"), true)} au ${formatDate(getText(el, "DateFin"), true)}`;
        items.push({ value, pageUrl: el.getAttribute("page") ?? undefined });
      }
    });
    next.compact = items[0]?.value ?? false;
    next.full = items;
  }

  if (contentType.title === "grades") {
    const items: ContentItem[] = [];
    eleve.querySelectorAll("PageReleveDeNotes Devoir").forEach((el) => {
      const note = getText(el, "Note");
      if (!note) return;
      items.push({
        value: `${note}/${getText(el, "Bareme")} en ${getText(el, "Matiere")} le ${formatDate(getText(el, "Date"))}`,
        pageUrl: el.getAttribute("page") ?? undefined,
      });
    });
    next.compact = items[0]?.value ?? false;
    next.full = items;
  }

  if (contentType.title === "diary") {
    const items: ContentItem[] = [];
    eleve.querySelectorAll("PageCahierDeTextes CahierDeTextes").forEach((diary) => {
      const matiere = getText(diary, "Matiere");
      const subsections = Array.from(diary.querySelectorAll("TravailAFaire"))
        .filter((work) => getText(work, "Descriptif"))
        .map((work) => ({
          header: `pour le ${formatDate(getText(work, "PourLe"))}`,
          content: new DOMParser().parseFromString(getText(work, "Descriptif"), "text/html").documentElement.textContent,
          pageUrl: work.getAttribute("page") ?? undefined,
        }));
      if (subsections.length > 0) {
        items.push({ value: `Nouveau devoir ${matiere}`, subsections });
      }
    });
    next.compact = items[0] ? `${items[0].value} ${items[0].subsections?.[0]?.header ?? ""}` : false;
    next.full = items;
  }

  if (contentType.title === "skills") {
    const items: ContentItem[] = [];
    eleve.querySelectorAll("PageCompetences Evaluation").forEach((el) => {
      const matiere = getText(el, "Matiere");
      const item = getText(el, "Item");
      let value = `${getText(el, "Intitule")} le ${formatDate(getText(el, "Date"))}`;
      if (matiere) value += ` en ${matiere}`;
      items.push({
        value,
        pageUrl: el.getAttribute("page") ?? undefined,
        subsections: [
          { header: "Compétence", content: getText(el, "Competence") },
          ...(item ? [{ header: "Item", content: item }] : []),
        ],
      });
    });
    next.compact = items[0]?.value ?? false;
    next.full = items;
  }

  return next;
};

/** Pure function — compute content types for a given eleve. Returns initial (all false) when eleve is null. */
export const computeContentTypes = (eleve: ParsedEleve | null): ContentType[] => {
  if (!eleve) return INITIAL_CONTENT_TYPES;
  return INITIAL_CONTENT_TYPES.map((ct) => computeForEleve(ct, eleve.element));
};

export const useCarnetDeBord = (): UseCarnetDeBordResult => {
  const { user } = useEdificeClient();

  const { data, isLoading, isError } = useQuery(carnetDeBordQueryOptions);

  const eleves = useMemo(() => {
    if (!data) return [];
    return parseEleves(data, (user?.children as Record<string, UserChild>) ?? {});
  }, [data, user?.children]);

  return { eleves, isLoading, isError };
};
