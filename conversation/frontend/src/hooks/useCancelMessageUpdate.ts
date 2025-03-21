import { useBeforeUnload, useBlocker } from 'react-router-dom';

export const useCancelMessageUpdate = (
  isDirty: boolean,
  handleBlockAction: () => void,
) => {
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      isDirty && currentLocation.pathname !== nextLocation.pathname,
  );

  const isBlocked = blocker.state === 'blocked';

  useBeforeUnload(async (event) => {
    if (isDirty) {
      await handleBlockAction();
      event.preventDefault();
    }
  });

  return { isBlocked };
};
