import { QueryClient } from '@tanstack/react-query';
import { useParams, Outlet } from 'react-router-dom';

export const loader =
  (_queryClient: QueryClient) =>
  async (/*{ params, request }: LoaderFunctionArgs*/) => {
    //const { folderId } = params;
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
