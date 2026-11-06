import { type SxProps, type Theme } from '@mui/material/styles';

/** Hide content visually while keeping it available to assistive technologies. */
export const visuallyHidden: SxProps<Theme> = {
  position: 'absolute',
  width: 1,
  height: 1,
  padding: 0,
  margin: -1,
  overflow: 'hidden',
  clip: 'rect(0 0 0 0)',
  whiteSpace: 'nowrap',
  border: 0,
};
