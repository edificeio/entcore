/**
 * Fixture for GET /directory/user/:userId/children (UserSpaceContainer).
 */
export const mockStructureChildren = [
  {
    structureName: 'Fake School',
    children: [
      {
        id: 'child-1',
        firstName: 'Léo',
        displayName: 'Léo Fake',
        externalId: 'ext-child-1',
        classesNames: ['CM2-A'],
      },
      {
        id: 'child-2',
        firstName: 'Léa',
        displayName: 'Léa Fake',
        externalId: 'ext-child-2',
        classesNames: ['CE2-B'],
      },
    ],
  },
];
