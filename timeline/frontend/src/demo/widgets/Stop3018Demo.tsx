import { Stop3018, Stop3018Modal } from '@edifice.io/react/homepage';
import { useState } from 'react';

/**
 * Demo "3018": the student card and its modal. The e-Enfance resources
 * (quiz, advice) do not open.
 */
export function Stop3018Demo() {
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <>
      <Stop3018
        audience="student"
        onLearnMoreClick={() => setIsModalOpen(true)}
        onLinkClick={() => {}}
      />
      <Stop3018Modal
        isOpen={isModalOpen}
        audience="student"
        onClose={() => setIsModalOpen(false)}
        onLinkClick={() => {}}
      />
    </>
  );
}
