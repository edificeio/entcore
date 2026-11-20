import {
  createMemoryRouter,
  RouterProvider,
  useParams,
} from 'react-router-dom';
import { HttpResponse, http } from 'msw';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { resetModerationMocks } from '~/mocks/moderation/handlers';
import { server } from '~/mocks/server';
import { act, fireEvent, render, screen, waitFor, within } from '~/mocks/setup';
import { queryClient } from '~/providers';
import { moderationBaseUrl } from '~/services/api';
import { moderationQueryKeys } from '~/services';
import { type Case } from '~/models';
import { useToastStore } from '~/store/toast';
import { ReportCaseProcess } from './ReportCaseProcess';

/** Mock case n°1: "Retour sur la kermesse #1" (blog), reported twice. */
const CASE_ID = 'case-0000-4000-8000-000000000001';
/** Mock case n°4, whose content is deleted. */
const DELETED_CASE_ID = 'case-0000-4000-8000-000000000004';
/** Mock case n°5, whose content is kept. */
const KEPT_CASE_ID = 'case-0000-4000-8000-000000000005';

const caseUrl = (caseId: string) => `/reports/structure-1/cases/${caseId}`;

/**
 * Renders the feature on its real route, next to stand-ins for the pages it
 * leads to (case detail, reports list). `history` puts entries before it.
 */
const renderProcess = (
  caseId = CASE_ID,
  history: string[] = [],
  // Stands in for the lazy route and loader of the real reports list
  listLoadDelay = 0,
) => {
  const ProcessRoute = () => {
    const { structureid = '', caseId: id = '' } = useParams();
    return <ReportCaseProcess caseId={id} structureId={structureid} />;
  };
  const router = createMemoryRouter(
    [
      {
        path: 'reports/:structureid',
        loader: () =>
          new Promise((resolve) =>
            setTimeout(() => resolve(null), listLoadDelay),
          ),
        element: <div>reports list</div>,
      },
      {
        path: 'reports/:structureid/cases/:caseId',
        element: <div>detail page</div>,
      },
      {
        path: 'reports/:structureid/cases/:caseId/process',
        element: <ProcessRoute />,
      },
    ],
    { initialEntries: [...history, `${caseUrl(caseId)}/process`] },
  );
  return { router, ...render(<RouterProvider router={router} />) };
};

describe('ReportCaseProcess', () => {
  beforeEach(() => {
    resetModerationMocks();
    queryClient.clear();
  });

  it('shows the case card as a level 2 heading, without its consult button', async () => {
    renderProcess();

    expect(
      await screen.findByRole('heading', {
        level: 2,
        name: 'Signalement - blog - Retour sur la kermesse #1',
      }),
    ).toBeVisible();
    expect(
      screen.queryByTestId('reports-case-consult-button'),
    ).not.toBeInTheDocument();
  });

  it('shows the decision and comments areas', async () => {
    renderProcess();

    expect(
      await screen.findByRole('heading', { level: 2, name: 'Décision' }),
    ).toBeVisible();
    expect(
      screen.getByText('Choisir une action pour ce contenu.'),
    ).toBeVisible();
    expect(
      screen.getByRole('heading', {
        level: 2,
        name: 'Commentaires (Facultatif)',
      }),
    ).toBeVisible();
    expect(
      screen.getByText(
        'Sélectionnez une décision pour afficher les champs de commentaire.',
      ),
    ).toBeVisible();
  });

  it('cancels back to the case detail page', async () => {
    renderProcess();

    expect(
      await screen.findByRole('link', { name: 'annuler' }),
    ).toHaveAttribute('href', `/reports/structure-1/cases/${CASE_ID}`);
  });

  it('keeps the submit button disabled', async () => {
    renderProcess();

    expect(
      await screen.findByRole('button', { name: 'Valider le traitement' }),
    ).toBeDisabled();
  });

  it('redirects to the case detail when the case cannot be processed', async () => {
    // Mock case n°4: its content is deleted
    const { router } = renderProcess(DELETED_CASE_ID);

    expect(await screen.findByText('detail page')).toBeVisible();
    expect(router.state.location.pathname).toBe(caseUrl(DELETED_CASE_ID));
  });

  it('shows an error when the case cannot be loaded', async () => {
    renderProcess('unknown-case');

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Impossible de charger le signalement.',
    );
    expect(
      screen.queryByRole('button', { name: 'Valider le traitement' }),
    ).not.toBeInTheDocument();
    // Still a way back to the case detail page
    expect(screen.getByRole('link', { name: 'annuler' })).toHaveAttribute(
      'href',
      '/reports/structure-1/cases/unknown-case',
    );
  });
});

/** Wait for the decision options, then check the one with this label. */
const chooseDecision = async (label: string) =>
  fireEvent.click(await screen.findByRole('radio', { name: label }));

/** Record the body of every decision request. */
const spyDecisions = () => {
  const bodies: unknown[] = [];
  server.use(
    http.post(
      `${moderationBaseUrl}/cases/:id/decisions`,
      async ({ request }) => {
        bodies.push(await request.json());
        return HttpResponse.json({ id: 'decision-1' });
      },
    ),
  );
  return bodies;
};

describe('ReportCaseProcess decision', () => {
  beforeEach(() => {
    resetModerationMocks();
    queryClient.clear();
  });

  afterEach(() => {
    act(() => useToastStore.getState().closeToast());
  });

  it('offers keep and delete, none selected, on a pending case', async () => {
    renderProcess();

    const radios = await screen.findAllByRole('radio');
    // In the Figma order, each named by its title and described by its text
    expect(radios).toHaveLength(2);
    expect(radios[0]).toBe(
      screen.getByRole('radio', { name: 'Conserver le contenu' }),
    );
    expect(radios[1]).toBe(
      screen.getByRole('radio', { name: 'Supprimer le contenu' }),
    );
    expect(radios[0]).toHaveAccessibleDescription(
      'Laisser le contenu accessible aux utilisateurs.',
    );
    radios.forEach((radio) => expect(radio).not.toBeChecked());
    expect(screen.queryByText('Mettre en quarantaine')).not.toBeInTheDocument();
  });

  it('only offers delete on a kept content', async () => {
    renderProcess(KEPT_CASE_ID);

    const radios = await screen.findAllByRole('radio');
    expect(radios).toHaveLength(1);
    expect(
      screen.getByRole('radio', { name: 'Supprimer le contenu' }),
    ).toBeInTheDocument();
  });

  it('shows the internal note once a decision is chosen', async () => {
    renderProcess();
    await screen.findAllByRole('radio');
    expect(
      screen.queryByRole('textbox', { name: 'Note interne' }),
    ).not.toBeInTheDocument();

    await chooseDecision('Conserver le contenu');

    expect(screen.getByRole('textbox', { name: 'Note interne' })).toBeVisible();
    expect(
      screen.getByText(
        'Cette note est visible uniquement par les administrateurs autorisés à consulter ce signalement.',
      ),
    ).toBeVisible();
    expect(
      screen.getByText(
        'Si vous le souhaitez, préciser la décision aux utilisateurs concernés.',
      ),
    ).toBeVisible();
  });

  it('enables submit once a decision is chosen', async () => {
    renderProcess();

    await chooseDecision('Conserver le contenu');

    expect(
      screen.getByRole('button', { name: 'Valider le traitement' }),
    ).toBeEnabled();
  });

  it('sends the decision with its note, then opens the processed tab with a success toast', async () => {
    const bodies = spyDecisions();
    const { router } = renderProcess();

    await chooseDecision('Supprimer le contenu');
    fireEvent.change(screen.getByRole('textbox', { name: 'Note interne' }), {
      target: { value: '  Contenu illicite ' },
    });
    fireEvent.click(
      screen.getByRole('button', { name: 'Valider le traitement' }),
    );

    expect(await screen.findByText('reports list')).toBeVisible();
    expect(bodies).toEqual([
      { status: 'RESOURCE_DELETED', comment: 'Contenu illicite' },
    ]);
    expect(router.state.location.pathname).toBe('/reports/structure-1');
    expect(router.state.location.search).toBe('?tab=settled');
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Contenu supprimé. Le signalement a été traité.',
    );
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('stays on the page with an error toast when the decision fails', async () => {
    server.use(
      http.post(`${moderationBaseUrl}/cases/:id/decisions`, () =>
        HttpResponse.json({ error: 'boom' }, { status: 500 }),
      ),
    );
    renderProcess();

    await chooseDecision('Conserver le contenu');
    fireEvent.change(screen.getByRole('textbox', { name: 'Note interne' }), {
      target: { value: 'RAS' },
    });
    fireEvent.click(
      screen.getByRole('button', { name: 'Valider le traitement' }),
    );

    expect(
      await screen.findByText(
        "Le traitement n'a pas pu être enregistré. Veuillez réessayer.",
      ),
    ).toBeVisible();
    expect(
      screen.getByRole('radio', { name: 'Conserver le contenu' }),
    ).toBeChecked();
    expect(screen.getByRole('textbox', { name: 'Note interne' })).toHaveValue(
      'RAS',
    );
    expect(screen.queryByText('reports list')).not.toBeInTheDocument();
  });

  it('sends a single request on a double click', async () => {
    const bodies = spyDecisions();
    renderProcess();

    await chooseDecision('Conserver le contenu');
    const submit = screen.getByRole('button', {
      name: 'Valider le traitement',
    });
    fireEvent.click(submit);
    fireEvent.click(submit);

    await screen.findByText('reports list');
    expect(bodies).toHaveLength(1);
  });
});

describe('ReportCaseProcess leave guard', () => {
  beforeEach(() => {
    resetModerationMocks();
    queryClient.clear();
  });

  it('leaves without asking when no decision is chosen', async () => {
    renderProcess();

    fireEvent.click(await screen.findByRole('link', { name: 'annuler' }));

    expect(await screen.findByText('detail page')).toBeVisible();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('asks before leaving with a chosen decision, and stays on "Poursuivre"', async () => {
    renderProcess();
    await chooseDecision('Supprimer le contenu');
    fireEvent.change(screen.getByRole('textbox', { name: 'Note interne' }), {
      target: { value: 'À revoir' },
    });

    fireEvent.click(screen.getByRole('link', { name: 'annuler' }));

    const dialog = await screen.findByRole('dialog', {
      name: 'Abandonner le traitement ?',
    });
    expect(dialog).toHaveAccessibleDescription(
      'La décision sélectionnée ne sera pas appliquée et les commentaires saisis seront perdus. Le signalement restera inchangé.',
    );

    fireEvent.click(screen.getByRole('button', { name: 'Poursuivre' }));

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    expect(
      screen.getByRole('radio', { name: 'Supprimer le contenu' }),
    ).toBeChecked();
    expect(screen.getByRole('textbox', { name: 'Note interne' })).toHaveValue(
      'À revoir',
    );
    expect(screen.queryByText('detail page')).not.toBeInTheDocument();
  });

  it('leaves on "Abandonner"', async () => {
    renderProcess();
    await chooseDecision('Conserver le contenu');

    fireEvent.click(screen.getByRole('link', { name: 'annuler' }));
    fireEvent.click(
      within(await screen.findByRole('dialog')).getByRole('button', {
        name: 'Abandonner',
      }),
    );

    expect(await screen.findByText('detail page')).toBeVisible();
  });

  it('asks on a browser back navigation', async () => {
    const { router } = renderProcess(CASE_ID, [caseUrl(CASE_ID)]);
    await chooseDecision('Conserver le contenu');

    act(() => {
      router.navigate(-1);
    });

    expect(await screen.findByRole('dialog')).toBeVisible();
    expect(router.state.location.pathname).toBe(`${caseUrl(CASE_ID)}/process`);
  });

  it('warns the browser before unloading, only with a chosen decision', async () => {
    renderProcess();
    await screen.findAllByRole('radio');

    const before = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(before);
    expect(before.defaultPrevented).toBe(false);

    await chooseDecision('Conserver le contenu');
    const after = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(after);
    expect(after.defaultPrevented).toBe(true);
  });
});

describe('ReportCaseProcess while the case changes', () => {
  beforeEach(() => {
    resetModerationMocks();
    queryClient.clear();
  });

  afterEach(() => {
    act(() => useToastStore.getState().closeToast());
  });

  it('redirects without asking when the case gets deleted meanwhile', async () => {
    renderProcess();
    await chooseDecision('Conserver le contenu');

    // Someone else deleted the content: a refetch brings the new status
    act(() => {
      queryClient.setQueryData<Case>(
        moderationQueryKeys.detail(CASE_ID),
        (current) => current && { ...current, status: 'RESOURCE_DELETED' },
      );
    });

    expect(await screen.findByText('detail page')).toBeVisible();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('opens the processed tab after a deletion, even when the list is slow to load', async () => {
    const { router } = renderProcess(CASE_ID, [], 300);
    // Every page reached, including those quickly replaced
    const visited: string[] = [];
    router.subscribe((state) => visited.push(state.location.pathname));

    await chooseDecision('Supprimer le contenu');
    fireEvent.click(
      screen.getByRole('button', { name: 'Valider le traitement' }),
    );

    expect(
      await screen.findByText('reports list', {}, { timeout: 2000 }),
    ).toBeVisible();
    expect(router.state.location.pathname).toBe('/reports/structure-1');
    expect(router.state.location.search).toBe('?tab=settled');
    // The refetch of the now deleted case must not redirect to its detail first
    expect(visited).not.toContain(caseUrl(CASE_ID));
  });

  it('disables cancel while the decision is being sent', async () => {
    server.use(
      http.post(`${moderationBaseUrl}/cases/:id/decisions`, async () => {
        await new Promise((resolve) => setTimeout(resolve, 200));
        return HttpResponse.json({ id: 'decision-1' });
      }),
    );
    renderProcess();

    await chooseDecision('Conserver le contenu');
    fireEvent.click(
      screen.getByRole('button', { name: 'Valider le traitement' }),
    );

    await waitFor(() =>
      expect(screen.getByRole('link', { name: 'annuler' })).toHaveAttribute(
        'aria-disabled',
        'true',
      ),
    );
    expect(await screen.findByText('reports list')).toBeVisible();
  });
});
