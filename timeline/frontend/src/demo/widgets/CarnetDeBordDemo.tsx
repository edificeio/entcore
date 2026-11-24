import {
  CarnetDeBord,
  parseEleves,
  Structure,
} from '@edifice.io/react/homepage';
import { useMemo } from 'react';

const pad = (value: number) => String(value).padStart(2, '0');

/** Local "YYYY-MM-DD" date `dayOffset` days away from today, as Pronote sends it. */
const pronoteDate = (dayOffset: number): string => {
  const date = new Date();
  date.setDate(date.getDate() + dayOffset);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};

/** Local "YYYY-MM-DDTHH:mm:ss" date-time, as Pronote sends it. */
const pronoteDateTime = (dayOffset: number, time: string): string =>
  `${pronoteDate(dayOffset)}T${time}:00`;

/**
 * Pronote export of a 5e student, dated around today. Only unjustified
 * lateness and absences are listed by the widget: the justified lateness
 * shows that it filters them out.
 */
const buildPronoteXml = (): string => `<?xml version="1.0" encoding="UTF-8"?>
<Pronote>
  <Eleve>
    <Prenom>Léa</Prenom>
    <Nom>Martin</Nom>
    <PageVieScolaire>
      <Retard page="vie-scolaire">
        <Date>${pronoteDateTime(-2, '08:10')}</Date>
        <Justifie>false</Justifie>
        <Motif>Retard de bus</Motif>
      </Retard>
      <Retard page="vie-scolaire">
        <Date>${pronoteDateTime(-6, '13:35')}</Date>
        <Justifie>true</Justifie>
      </Retard>
      <Absence page="vie-scolaire">
        <DateDebut>${pronoteDateTime(-9, '08:00')}</DateDebut>
        <DateFin>${pronoteDateTime(-9, '12:15')}</DateFin>
        <EstOuverte>false</EstOuverte>
        <Justifie>false</Justifie>
        <Motif>Rendez-vous médical</Motif>
      </Absence>
    </PageVieScolaire>
    <PageReleveDeNotes>
      <Devoir page="notes">
        <Note>16,50</Note>
        <Bareme>20</Bareme>
        <Matiere>MATHEMATIQUES</Matiere>
        <Date>${pronoteDate(-1)}</Date>
      </Devoir>
      <Devoir page="notes">
        <Note>14,00</Note>
        <Bareme>20</Bareme>
        <Matiere>FRANCAIS</Matiere>
        <Date>${pronoteDate(-4)}</Date>
      </Devoir>
      <Devoir page="notes">
        <Note>17,00</Note>
        <Bareme>20</Bareme>
        <Matiere>ANGLAIS LV1</Matiere>
        <Date>${pronoteDate(-7)}</Date>
      </Devoir>
      <Devoir page="notes">
        <Note>12,50</Note>
        <Bareme>20</Bareme>
        <Matiere>HISTOIRE-GEOGRAPHIE</Matiere>
        <Date>${pronoteDate(-11)}</Date>
      </Devoir>
    </PageReleveDeNotes>
    <PageCahierDeTextes>
      <CahierDeTextes>
        <Matiere>MATHEMATIQUES</Matiere>
        <TravailAFaire page="cdt">
          <PourLe>${pronoteDate(1)}</PourLe>
          <Descriptif>&lt;p&gt;Exercices 12 et 13 page 87 sur les fractions&lt;/p&gt;</Descriptif>
        </TravailAFaire>
      </CahierDeTextes>
      <CahierDeTextes>
        <Matiere>FRANCAIS</Matiere>
        <TravailAFaire page="cdt">
          <PourLe>${pronoteDate(2)}</PourLe>
          <Descriptif>&lt;p&gt;Lire les chapitres 3 et 4 du roman étudié en classe&lt;/p&gt;</Descriptif>
        </TravailAFaire>
      </CahierDeTextes>
      <CahierDeTextes>
        <Matiere>HISTOIRE-GEOGRAPHIE</Matiere>
        <TravailAFaire page="cdt">
          <PourLe>${pronoteDate(3)}</PourLe>
          <Descriptif>&lt;p&gt;Apprendre la leçon sur l’empire de Charlemagne&lt;/p&gt;</Descriptif>
        </TravailAFaire>
      </CahierDeTextes>
    </PageCahierDeTextes>
    <PageCompetences>
      <Evaluation page="competences">
        <Intitule>Évaluation</Intitule>
        <Date>${pronoteDate(-3)}</Date>
        <Matiere>SCIENCES DE LA VIE ET DE LA TERRE</Matiere>
        <Competence>Pratiquer des démarches scientifiques</Competence>
        <Item>Formuler une hypothèse</Item>
      </Evaluation>
      <Evaluation page="competences">
        <Intitule>Exposé oral</Intitule>
        <Date>${pronoteDate(-8)}</Date>
        <Matiere>ANGLAIS LV1</Matiere>
        <Competence>S’exprimer à l’oral en continu</Competence>
      </Evaluation>
    </PageCompetences>
  </Eleve>
</Pronote>`;

/**
 * Demo "Carnet de bord", student view. No Pronote address: the modal's
 * "open Pronote" button, which would leave the demo, is not displayed.
 */
export function CarnetDeBordDemo() {
  const eleves = useMemo(() => {
    const structures: Structure[] = [
      {
        structureId: 'demo-structure',
        xmlResponse: buildPronoteXml(),
        address: '',
      },
    ];
    return parseEleves(structures, {});
  }, []);

  return (
    <CarnetDeBord
      status="default"
      eleves={eleves}
      currentEleveIndex={0}
      onEleveChange={() => {}}
      isRelative={false}
    />
  );
}
