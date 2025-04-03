/**
 * Test suite for the RenameFolderModal component.
 *
 * This suite includes tests to verify the following functionalities:
 * - Successful rendering of the component.
 * - Validation to prevent creating a folder with only blank characters.
 * - Validation to prevent creating a folder with a duplicate name.
 * - Successful update of a folder with a valid name.
 *
 * Mocks:
 * - Mock `success` and `error` functions from the `useToast` hook.
 *
 * Tests:
 * - `should render successfully`: Verifies that the component renders correctly with required elements.
 * - `should forbid renaming a folder named with only blank characters`: Ensures that renaming a folder with only blank characters triggers an error toast.
 * - `should forbid renaming a folder with the same name as another folder`: Ensures that renaming a folder with a duplicate name triggers an error toast.
 * - `should rename a folder`: Ensures that creating a folder with a valid name triggers a success toast.
 */
import { describe, expect, it } from 'vitest';
import { fireEvent, render, renderHook, screen, waitFor } from '~/mocks/setup';
import { RenameFolderModal } from './RenameFolderModal';
import { mockFolderTree } from '~/mocks';
import { useAppActions } from '~/store';

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

describe('RenameFolderModal component', () => {
  beforeEach(async () => {
    const { result } = renderHook(useAppActions);
    const setSelectedFolders = await waitFor(() => {
      expect(result.current.setSelectedFolders).toBeDefined();
      return result.current.setSelectedFolders;
    });
    setSelectedFolders([mockFolderTree[1]]); // Select folder_B to rename it
  });

  afterEach(async () => {
    const { result } = renderHook(useAppActions);
    const setSelectedFolders = await waitFor(() => {
      expect(result.current.setSelectedFolders).toBeDefined();
      return result.current.setSelectedFolders;
    });
    setSelectedFolders([]);
    vi.clearAllMocks();
  });

  it('should render successfully', async () => {
    const { baseElement } = render(<RenameFolderModal />);
    expect(baseElement).toBeTruthy();

    const inputNewName = await screen.findByPlaceholderText<HTMLInputElement>(
      'folder.rename.name.placeholder',
    );
    expect(inputNewName).toBeRequired();
  });

  it('should forbid renaming a folder named with only blank characters', async () => {
    render(<RenameFolderModal />);
    const inputNewName = await screen.findByPlaceholderText<HTMLInputElement>(
      'folder.rename.name.placeholder',
    );
    fireEvent.change(inputNewName, { target: { value: '   ' } });
    expect(inputNewName.value).toStrictEqual('   ');

    const btnSave = await screen.findByText<HTMLButtonElement>('save');
    fireEvent.click(btnSave);

    // Wait for one event loop
    await new Promise((r) => setTimeout(r, 0));

    expect(mocks.useToast.error).toHaveBeenCalledTimes(1);
  });

  it('should forbid renaming a folder with the same name as another folder', async () => {
    render(<RenameFolderModal />);
    const inputNewName = await screen.findByPlaceholderText<HTMLInputElement>(
      'folder.rename.name.placeholder',
    );
    fireEvent.change(inputNewName, { target: { value: 'Root folder A' } });
    expect(inputNewName.value).toStrictEqual('Root folder A');

    const btnSave = await screen.findByText<HTMLButtonElement>('save');
    fireEvent.click(btnSave);

    // Wait for one event loop
    await new Promise((r) => setTimeout(r, 0));

    expect(mocks.useToast.error).toHaveBeenCalledTimes(1);
  });

  it('should rename a folder', async () => {
    render(<RenameFolderModal />);
    const inputNewName = await screen.findByPlaceholderText<HTMLInputElement>(
      'folder.rename.name.placeholder',
    );
    fireEvent.change(inputNewName, { target: { value: 'Ja Ja !' } });

    const btnSave = await screen.findByText<HTMLButtonElement>('save');
    fireEvent.click(btnSave);

    // Wait for one event loop
    await new Promise((r) => setTimeout(r, 500));

    expect(mocks.useToast.success).toHaveBeenCalledTimes(1);
  });
});
