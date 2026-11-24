import { UsefulLink } from '@edifice.io/client';
import { UsefulLinks, UsefulLinksModal } from '@edifice.io/react/homepage';
import { useState } from 'react';

const LINKS: UsefulLink[] = [
  {
    id: 'demo-lumni',
    name: 'Lumni : réviser au collège',
    url: 'https://www.lumni.fr/college',
  },
  {
    id: 'demo-vikidia',
    name: 'Vikidia, l’encyclopédie des 8-13 ans',
    url: 'https://fr.vikidia.org',
  },
  {
    id: 'demo-mathenpoche',
    name: 'Sésamath : Mathenpoche 5e',
    url: 'https://mathenpoche.sesamath.net',
  },
  {
    id: 'demo-onisep',
    name: 'Onisep : découvrir les métiers',
    url: 'https://www.onisep.fr',
  },
];

/**
 * Demo "Liens utiles": a student's bookmarks. The management modal opens,
 * adding, editing and deleting links do nothing.
 */
export function UsefulLinksDemo() {
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <>
      <UsefulLinks links={LINKS} onEditClick={() => setIsModalOpen(true)} />
      <UsefulLinksModal
        isOpen={isModalOpen}
        links={LINKS}
        canAddLink
        onClose={() => setIsModalOpen(false)}
        onAddClick={() => {}}
        onEditLink={() => {}}
        onDeleteLink={() => {}}
      />
    </>
  );
}
