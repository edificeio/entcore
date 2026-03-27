/** Check old format URL and redirect if needed */
export const manageRedirections = (): string | null => {
  const pathLocation = window.location.pathname;

  if (
    pathLocation === '/' ||
    pathLocation === '/timeline' ||
    pathLocation === '/timeline/' ||
    pathLocation === '/timeline/timeline'
  ) {
    return '/';
  }

  // No redirection needed
  return null;
};
