import {
  Stop3018,
  Stop3018Link,
  Stop3018Modal,
} from '@edifice.io/react/homepage';
import { useState } from 'react';

// Student resources of the e-Enfance association, as in the framework
// container (its `openStop3018Link` helper is not exported).
const E_ENFANCE_ADDRESS = 'https://e-enfance.org';
const STUDENT_LINKS: Record<Stop3018Link, string> = {
  harassment: `${E_ENFANCE_ADDRESS}/informer/harcelement-scolaire/`,
  faq: `${E_ENFANCE_ADDRESS}/f-a-q-conseils-pour-les-jeunes/`,
  quiz: `${E_ENFANCE_ADDRESS}/harcelometre/`,
};

const openLink = (link: Stop3018Link) =>
  window.open(STUDENT_LINKS[link], '_blank', 'noopener,noreferrer');

/**
 * Demo "3018": the student card and its modal. The e-Enfance resources
 * (quiz, advice) open in a new tab, like on the real widget.
 */
export function Stop3018Demo() {
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <>
      <Stop3018
        audience="student"
        onLearnMoreClick={() => setIsModalOpen(true)}
        onLinkClick={openLink}
      />
      <Stop3018Modal
        isOpen={isModalOpen}
        audience="student"
        onClose={() => setIsModalOpen(false)}
        onLinkClick={openLink}
      />
    </>
  );
}
