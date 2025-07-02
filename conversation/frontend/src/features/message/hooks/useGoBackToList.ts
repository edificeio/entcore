import { useNavigate, useSearchParams } from 'react-router-dom';
import { useScrollStore } from '~/store/scrollStore';

export const useGoBackToList = () => {
  const navigate = useNavigate();
  const savedScrollPosition = useScrollStore.use.savedScrollPosition();
  const [searchParams] = useSearchParams();

  const goBackToList = () => {
    navigate(
      {
        pathname: `../..`,
        search: searchParams.toString(),
      },
      {
        relative: 'path',
        state: {
          scrollPositionToRestore: savedScrollPosition,
        },
      },
    );
  };

  return {
    goBackToList,
  };
};
