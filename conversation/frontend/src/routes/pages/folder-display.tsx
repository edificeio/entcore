import { QueryClient } from '@tanstack/react-query';
import { useParams, LoaderFunctionArgs, Outlet } from 'react-router-dom';

export const loader =
  (queryClient: QueryClient) =>
  async ({ params, request }: LoaderFunctionArgs) => {
    const { folderId } = params;
    return null;
  };

export function Component() {
  const { folderId } = useParams();

  return (
    <>
      {folderId}
      <Outlet />
    </>
  );
}
