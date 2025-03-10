import { Folder, Message, MessageMetadata } from '~/models';

export const mockFolderTree: Array<Folder> = [
  {
    id: 'folder_A',
    parent_id: null,
    name: 'Root folder A',
    depth: 1,
    nbMessages: 1,
    nbUnread: 0,
    subFolders: [
      {
        id: 'folder_A_1',
        parent_id: 'folder_A',
        name: 'Sub Folder A.1',
        depth: 2,
        nbMessages: 1,
        nbUnread: 1,
      },
    ],
  },
  {
    id: 'folder_B',
    parent_id: null,
    name: 'Root folder B',
    depth: 1,
    nbMessages: 0,
    nbUnread: 0,
  },
];

export const mockCountOfMessagesInInbox = { count: 2 };

export const mockMessagesOfInbox: MessageMetadata[] = [
  {
    id: 'f43d3783',
    subject: 'Prêt des manuels scolaires',
    from: {
      id: '91c22b66',
      displayName: 'ISABELLE POLONIO (prof arts plastiques)',
      profile: 'Teacher',
    },
    state: 'SENT',
    date: 1503571892555,
    unread: false,
    response: false,
    trashed: false,
    forwarded: false,
    hasAttachment: false,
    to: {
      users: [],
      groups: [
        {
          id: '465',
          displayName: 'Enseignants du groupe scolaire.',
          size: 42,
          type: 'ProfileGroup',
          subType: 'StructureGroup',
        },
        {
          id: '467',
          displayName: 'Parents du groupe scolaire.',
          size: 1043,
          type: 'ProfileGroup',
          subType: 'StructureGroup',
        },
        {
          id: '468',
          displayName: 'Élèves du groupe scolaire.',
          size: 577,
          type: 'ProfileGroup',
          subType: 'StructureGroup',
        },
        {
          id: '466',
          displayName: 'Personnels du groupe scolaire.',
          size: 22,
          type: 'ProfileGroup',
          subType: 'StructureGroup',
        },
      ],
    },
    cc: {
      users: [],
      groups: [],
    },
    cci: {
      users: [],
      groups: [],
    },
  },
  {
    id: '4d14920b',
    subject: 'Prochain Voyage',
    from: {
      id: '91c22b66',
      displayName: 'ISABELLE POLONIO (prof arts plastiques)',
      profile: 'Teacher',
    },
    state: 'SENT',
    date: 1475753026475,
    unread: true,
    response: false,
    hasAttachment: false,
    trashed: false,
    forwarded: false,
    to: {
      users: [],
      groups: [
        {
          id: '467',
          displayName: 'Parents du groupe scolaire.',
          size: 1043,
          type: 'ProfileGroup',
          subType: 'StructureGroup',
        },
      ],
    },
    cc: {
      users: [],
      groups: [],
    },
    cci: {
      users: [],
      groups: [],
    },
  },
];

export const mockMessageOfOutbox: MessageMetadata = {
  id: 'f43d3783',
  subject: 'Prêt des manuels scolaires',
  from: {
    displayName: 'LOISON Stéphane',
    id: 'b92e3d37-16b0-4ed9-b4c3-992091687132',
    profile: 'Teacher',
  },
  state: 'SENT',
  date: 1503571892555,
  unread: true,
  response: false,
  trashed: false,
  forwarded: false,
  hasAttachment: false,
  to: {
    users: [],
    groups: [
      {
        id: '465',
        displayName: 'Enseignants du groupe scolaire.',
        size: 42,
        type: 'ProfileGroup',
        subType: 'StructureGroup',
      },
      {
        id: '468',
        displayName: 'Élèves du groupe scolaire.',
        size: 577,
        type: 'ProfileGroup',
        subType: 'StructureGroup',
      },
    ],
  },
  cc: {
    users: [
      {
        id: '91c22b66',
        displayName: 'ISABELLE POLONIO (prof arts plastiques)',
        profile: 'Teacher',
      },
    ],
    groups: [],
  },
  cci: {
    users: [
      {
        displayName: 'LOISON Stéphane',
        id: 'b92e3d37-16b0-4ed9-b4c3-992091687132',
        profile: 'Teacher',
      },
    ],
    groups: [
      {
        id: '467',
        displayName: 'Parents du groupe scolaire.',
        size: 1043,
        type: 'ProfileGroup',
        subType: 'StructureGroup',
      },
    ],
  },
};

export const mockFullMessage: Message = {
  id: 'f43d3783',
  subject: 'Prêt des manuels scolaires',
  from: {
    displayName: 'GUEDON Aliénor',
    id: '8f82fb3c-2150-4a1f-a869-33e98b8b836f',
    profile: 'Student',
  },
  state: 'SENT',
  date: 1503571892555,
  unread: true,
  response: false,
  attachments: [
    {
      id: 'acc05149-3858-49f0-a436-ef129d8cb621',
      name: 'file',
      charset: 'UTF-8',
      filename: 'Autorisation sortie scolaire.pdf',
      contentType: 'application/pdf',
      contentTransferEncoding: '7bit',
      size: 37449,
    },
    {
      id: 'acc05149-3858-49f0-a436-zqzd',
      name: 'file',
      charset: 'UTF-8',
      filename: 'Yolo.pdf',
      contentType: 'application/pdf',
      contentTransferEncoding: '7bit',
      size: 37449,
    },
  ],
  to: {
    users: [
      {
        displayName: 'LOISON Stéphane',
        id: 'b92e3d37-16b0-4ed9-b4c3-992091687132',
        profile: 'Teacher',
      },
      {
        id: '91c22b66',
        displayName: 'ISABELLE POLONIO (prof arts plastiques)',
        profile: 'Teacher',
      },
      {
        displayName: 'GUEDON Céline',
        id: 'c0824335-ab0e-41fb-9ed3-d5c28a93087d',
        profile: 'Relative',
      },
    ],
    groups: [
      {
        id: '465',
        displayName: 'Enseignants du groupe scolaire.',
        size: 42,
        type: 'ProfileGroup',
        subType: 'StructureGroup',
      },
      {
        id: '467',
        displayName: 'Parents du groupe scolaire.',
        size: 1043,
        type: 'ProfileGroup',
        subType: 'StructureGroup',
      },
      {
        id: '468',
        displayName: 'Élèves du groupe scolaire.',
        size: 577,
        type: 'ProfileGroup',
        subType: 'StructureGroup',
      },
      {
        id: '466',
        displayName: 'Personnels du groupe scolaire.',
        size: 22,
        type: 'ProfileGroup',
        subType: 'StructureGroup',
      },
    ],
  },
  cc: {
    users: [
      {
        displayName: 'CARPENTIER Béatrice',
        id: 'c0824335-ab0e-41fb-9ed3-d5c28a93087d',
        profile: 'Relative',
      },
    ],
    groups: [],
  },
  cci: {
    users: [],
    groups: [],
  },
  body: 'Bonjour, à propos du prochain voyage...',
  language: 'fr',
  folder_id: '',
  parent_id: '',
  thread_id: '',
  trashed: false,
  forwarded: false,
};

export const mockSentMessage = {
  sent: 1,
  undelivered: [],
  inactive: [],
  thread_id: null,
};
