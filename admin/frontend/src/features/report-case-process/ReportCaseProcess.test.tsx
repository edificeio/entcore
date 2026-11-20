import {
  createMemoryRouter,
  RouterProvider,
  useParams,
} from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';

import { resetModerationMocks } from '~/mocks/moderation/handlers';
import { render, screen } from '~/mocks/setup';
import { queryClient } from '~/providers';
import { ReportCaseProcess } from './ReportCaseProcess';

/** Mock case n°1: "Retour sur la kermesse #1" (blog), reported twice. */
const CASE_ID = 'case-0000-4000-8000-000000000001';
/** Mock case n°4, whose content is deleted. */
const DELETED_CASE_ID = 'case-0000-4000-8000-000000000004';

const caseUrl = (caseId: string) => `/reports/structure-1/cases/${caseId}`;

/**
 * Renders the feature on its real route, next to stand-ins for the pages it
 * leads to (case detail, reports list). `history` puts entries before it.
 */
const renderProcess = (caseId = CASE_ID, history: string[] = []) => {
  const ProcessRoute = () => {
    const { structureid = '', caseId: id = '' } = useParams();
    return <ReportCaseProcess caseId={id} structureId={structureid} />;
  };
  const router = createMemoryRouter(
    [
      { path: 'reports/:structureid', element: <div>reports list</div> },
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
