/**
 * Test suite for the CreateFolderModal component.
 *
 * This suite includes tests to verify the following functionalities:
 * - Successful rendering of the component.
 * - Validation to prevent creating a folder with only blank characters.
 * - Validation to prevent creating a folder with a duplicate name.
 * - Successful creation of a folder with a valid name.
 *
 * Mocks:
 * - Mock `success` and `error` functions from the `useToast` hook.
 *
 * Tests:
 * - `should render successfully`: Verifies that the component renders correctly with required elements.
 * - `should forbid creating a folder named with only blank characters`: Ensures that creating a folder with only blank characters triggers an error toast.
 * - `should forbid creating a folder with the same name as another folder`: Ensures that creating a folder with a duplicate name triggers an error toast.
 * - `should create a folder`: Ensures that creating a folder with a valid name triggers a success toast.
 */
import { describe, expect, it } from 'vitest';
import { fireEvent, render, screen } from '~/mocks/setup';
import { CreateFolderModal } from './CreateFolderModal';

/**
 * Mock `success` and `error` functions from useToast hook.
 */
const mocks = vi.hoisted(() => ({
  useToast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

vi.mock('@edifice.io/react', async () => {
  const actual =
    await vi.importActual<typeof import('@edifice.io/react')>(
      '@edifice.io/react',
    );
  return {
    ...actual,
    useToast: () => {
      const useToast = actual.useToast();
      return {
        ...useToast,
        success: mocks.useToast.success,
        error: mocks.useToast.error,
      };
    },
  };
});

describe('CreateFolderModal component', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should render successfully', async () => {
    const { baseElement } = render(<CreateFolderModal />);
    expect(baseElement).toBeTruthy();

    const inputNewName = await screen.findByPlaceholderText<HTMLInputElement>(
      'folder.new.name.placeholder',
    );
    expect(inputNewName).toBeRequired();

    const checkParentFolder = await screen.findByLabelText(
      'folder.new.subfolder.label',
    );
    expect(checkParentFolder).not.toBeChecked();

    const dropdownParentFolder = await screen.findByText<HTMLButtonElement>(
      'folder.new.subfolder.placeholder',
    );
    expect(dropdownParentFolder).toBeInTheDocument();
    expect(dropdownParentFolder).toBeDisabled();
  });

  it('should forbid creating a folder named with only blank characters', async () => {
    render(<CreateFolderModal />);
    const inputNewName = await screen.findByPlaceholderText<HTMLInputElement>(
      'folder.new.name.placeholder',
    );
    fireEvent.change(inputNewName, { target: { value: '   ' } });
    expect(inputNewName.value).toStrictEqual('   ');

    const btnCreate = await screen.findByText<HTMLButtonElement>('create');
    fireEvent.click(btnCreate);

    // Wait for one event loop
    await new Promise((r) => setTimeout(r, 0));

    expect(mocks.useToast.error).toHaveBeenCalledTimes(1);
  });

  it('should forbid creating a folder with the same name as another folder', async () => {
    render(<CreateFolderModal />);
    const inputNewName = await screen.findByPlaceholderText<HTMLInputElement>(
      'folder.new.name.placeholder',
    );
    fireEvent.change(inputNewName, { target: { value: 'Root folder A' } });
    expect(inputNewName.value).toStrictEqual('Root folder A');

    const btnCreate = await screen.findByText<HTMLButtonElement>('create');
    fireEvent.click(btnCreate);

    // Wait for one event loop
    await new Promise((r) => setTimeout(r, 0));

    await screen.findByText('conversation.error.duplicate.folder');
  });

  it('should create a folder', async () => {
    render(<CreateFolderModal />);
    const inputNewName = await screen.findByPlaceholderText<HTMLInputElement>(
      'folder.new.name.placeholder',
    );
    fireEvent.change(inputNewName, { target: { value: 'Ja Ja !' } });

    const btnCreate = await screen.findByText<HTMLButtonElement>('create');
    fireEvent.click(btnCreate);

    // Wait for one event loop
    await new Promise((r) => setTimeout(r, 500));

    expect(mocks.useToast.success).toHaveBeenCalledTimes(1);
  });
});
