import { matchPath } from 'react-router-dom';

/** Check old format URL and redirect if needed */
export const manageRedirections = (): string | null => {
  const hashLocation = window.location.hash.substring(1);

  if (hashLocation) {
    let redirectPath = '';
    const isPath = matchPath('/view/:id', hashLocation);

    if (isPath) {
      // Redirect to the new format
      redirectPath = `/id/${isPath?.params.id}`;
    }

    return redirectPath;
  }

  // No redirection needed
  return null;
};
