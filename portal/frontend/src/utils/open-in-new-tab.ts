export const openInNewTab = (url?: string | null) => (e: React.MouseEvent) => {
  e.preventDefault();
  if (url) window.open(url, '_blank', 'noopener,noreferrer');
};