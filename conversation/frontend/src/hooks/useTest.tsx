import { useParams } from 'react-router-dom';

export function useTest() {
  const { folderId } = useParams();
  return { folderId };
}
