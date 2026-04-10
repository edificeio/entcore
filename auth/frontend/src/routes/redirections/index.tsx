/** Check old format URL and redirect if needed */
export const manageRedirections = (): string | null => {
  const pathLocation = window.location.pathname;

  if (
    pathLocation === '/' ||
    pathLocation === '/auth' ||
    pathLocation === '/auth/' ||
    pathLocation === '/auth/auth'
  ) {
    return '/auth';
  }

  // No redirection needed
  return null;
};
