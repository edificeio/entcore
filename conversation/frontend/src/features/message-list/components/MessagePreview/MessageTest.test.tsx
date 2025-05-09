import { render, screen } from '~/mocks/setup';
// import { useTest } from '~/hooks/useTest';
import { useTest } from '~/hooks';

export function MessageTest() {
  const { folderId } = useTest();
  return <p>{folderId}</p>;
}

vi.mock('react-router-dom', async () => {
  const actual =
    await vi.importActual<typeof import('react-router-dom')>(
      'react-router-dom',
    );
  return {
    ...actual,
    useParams: () => {
      return {
        folderId: 'test-folder',
      };
    },
  };
});

describe('', () => {
  it('', async () => {
    render(<MessageTest />);
    await screen.findByText('test-folder');
  });
});
