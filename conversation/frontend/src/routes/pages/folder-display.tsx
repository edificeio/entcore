import { QueryClient } from '@tanstack/react-query';
import { Outlet } from 'react-router-dom';
import { useSelectedFolder } from '~/hooks';

export const loader = (_queryClient: QueryClient) => async () => {
  return null;
};

export function Component() {
  const { folderId } = useSelectedFolder();

  return (
    <>
      TODO {folderId}
      <Outlet />
    </>
  );
}
