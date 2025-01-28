import { describe, expect, it } from 'vitest';
import { render, screen } from '~/mocks/setup';
import { CreateFolderModal } from './CreateFolderModal';

describe('CreateFolderModal component', () => {
  it('should render successfully', async () => {
    const { baseElement } = render(<CreateFolderModal />);

    expect(baseElement).toBeTruthy();

    const inputNewName = await screen.findByTestId('inputNewName');
    expect(inputNewName).toBeInTheDocument();
    expect(inputNewName).toBeRequired();

    const checkParentFolder = await screen.findByTestId('checkParentFolder');
    expect(checkParentFolder).toBeInTheDocument();
    expect(checkParentFolder).not.toBeChecked();
  });
});
