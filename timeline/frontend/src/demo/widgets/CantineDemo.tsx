import { School } from '@edifice.io/client';
import {
  Cantine,
  CantineDish,
  CantineModal,
  CantineSection,
} from '@edifice.io/react/homepage';
import { useState } from 'react';

const SCHOOL: School = {
  id: 'demo-school',
  name: 'Collège Jean Moulin',
  UAI: '0590001A',
  classes: ['5e B'],
  exports: [],
};

/** How far the modal lets the user browse around today, in days. */
const DATE_RANGE_IN_DAYS = 7;

const dish = (
  id: string,
  label: string,
  overrides: Partial<CantineDish> = {},
): CantineDish => ({
  id,
  label,
  vegetarien: false,
  faitmaison: false,
  bio: false,
  local: false,
  allergens: [],
  ...overrides,
});

/** Menus served in turn, one per day. */
const MENUS: CantineSection[][] = [
  [
    {
      category: 'entree',
      items: [
        dish('carottes', 'Carottes râpées vinaigrette', {
          vegetarien: true,
          bio: true,
          allergens: ['moutarde'],
        }),
        dish('betteraves', 'Salade de betteraves', { vegetarien: true }),
      ],
    },
    {
      category: 'plat',
      items: [
        dish('poulet', 'Sauté de poulet au curry', {
          faitmaison: true,
          local: true,
          allergens: ['lait'],
        }),
      ],
    },
    {
      category: 'accompagnement',
      items: [
        dish('riz', 'Riz pilaf', { vegetarien: true, bio: true }),
        dish('haricots', 'Haricots verts persillés', {
          vegetarien: true,
          local: true,
        }),
      ],
    },
    {
      category: 'laitage',
      items: [
        dish('yaourt', 'Yaourt nature', {
          bio: true,
          local: true,
          allergens: ['lait'],
        }),
      ],
    },
    {
      category: 'dessert',
      items: [
        dish('pomme', 'Pomme des vergers de Flandre', {
          vegetarien: true,
          local: true,
        }),
      ],
    },
  ],
  [
    {
      category: 'entree',
      items: [
        dish('potage', 'Potage de légumes', {
          vegetarien: true,
          faitmaison: true,
          local: true,
          allergens: ['celeri'],
        }),
      ],
    },
    {
      category: 'plat',
      items: [
        dish('lasagnes', 'Lasagnes à la bolognaise', {
          faitmaison: true,
          allergens: ['gluten', 'lait', 'oeuf'],
        }),
        dish('lasagnes-vege', 'Lasagnes aux légumes du soleil', {
          vegetarien: true,
          faitmaison: true,
          allergens: ['gluten', 'lait', 'oeuf'],
        }),
      ],
    },
    {
      category: 'accompagnement',
      items: [dish('salade', 'Salade verte', { vegetarien: true, bio: true })],
    },
    {
      category: 'laitage',
      items: [
        dish('emmental', 'Emmental', { allergens: ['lait'] }),
        dish('maroilles', 'Maroilles', { local: true, allergens: ['lait'] }),
      ],
    },
    {
      category: 'dessert',
      items: [
        dish('compote', 'Compote pomme-poire', {
          vegetarien: true,
          faitmaison: true,
          bio: true,
        }),
      ],
    },
  ],
  [
    {
      category: 'entree',
      items: [
        dish('concombre', 'Concombre à la crème', {
          vegetarien: true,
          bio: true,
          allergens: ['lait'],
        }),
      ],
    },
    {
      category: 'plat',
      items: [
        dish('poisson', 'Filet de colin sauce citron', {
          allergens: ['poisson', 'lait'],
        }),
      ],
    },
    {
      category: 'accompagnement',
      items: [
        dish('pommes-vapeur', 'Pommes de terre vapeur', {
          vegetarien: true,
          local: true,
        }),
      ],
    },
    {
      category: 'laitage',
      items: [dish('fromage-blanc', 'Fromage blanc', { allergens: ['lait'] })],
    },
    {
      category: 'dessert',
      items: [
        dish('gateau', 'Gâteau au chocolat', {
          vegetarien: true,
          faitmaison: true,
          allergens: ['gluten', 'oeuf', 'lait'],
        }),
      ],
    },
  ],
];

const menuOf = (dayOffset: number): CantineSection[] =>
  MENUS[((dayOffset % MENUS.length) + MENUS.length) % MENUS.length];

/** Local "YYYY-MM-DD" date, `dayOffset` days away from today. */
const dayFromToday = (dayOffset: number): string => {
  const date = new Date();
  date.setDate(date.getDate() + dayOffset);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
};

/**
 * Demo "Cantine": today's lunch menu of a collège without dinner service.
 * The full-screen modal browses a few menus served in turn around today.
 */
export function CantineDemo() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [dayOffset, setDayOffset] = useState(0);

  const openModal = () => {
    setDayOffset(0);
    setIsModalOpen(true);
  };

  return (
    <>
      <Cantine
        status="default"
        sections={menuOf(0)}
        handleFullScreenClick={openModal}
      />
      <CantineModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        schools={[SCHOOL]}
        selectedSchool={SCHOOL}
        onSchoolChange={() => {}}
        hasDinner={false}
        menuType="lunch"
        onMenuTypeChange={() => {}}
        date={dayFromToday(dayOffset)}
        canGoPrevious={dayOffset > -DATE_RANGE_IN_DAYS}
        canGoNext={dayOffset < DATE_RANGE_IN_DAYS}
        onPreviousDay={() => setDayOffset((offset) => offset - 1)}
        onNextDay={() => setDayOffset((offset) => offset + 1)}
        sections={menuOf(dayOffset)}
        status="default"
      />
    </>
  );
}
