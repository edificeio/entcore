import { type ReactNode } from 'react';

import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';

/**
 * Line height of the info rows: 24px of text plus the 8px Figma puts between
 * rows and between the wrapped lines of a row.
 */
const INFO_LINE_HEIGHT = 32;

export interface InfoRowProps {
  label: string;
  /** Cut the value with an ellipsis after this number of lines. */
  maxLines?: number;
  children: ReactNode;
}

/**
 * One "label : value" pair of the enclosing list. Term and description flow as
 * one inline text, so a long value wraps after its label instead of below it.
 */
export const InfoRow = ({ label, maxLines, children }: InfoRowProps) => (
  <Typography
    component="div"
    variant="body1"
    sx={{
      lineHeight: `${INFO_LINE_HEIGHT}px`,
      overflowWrap: 'anywhere',
      ...(maxLines && {
        display: '-webkit-box',
        WebkitBoxOrient: 'vertical',
        WebkitLineClamp: maxLines,
        // Clipped, not removed: screen readers still read the whole value
        overflow: 'hidden',
      }),
    }}
  >
    <Box
      component="dt"
      sx={{ display: 'inline', fontWeight: 'fontWeightBold' }}
    >
      {label}&nbsp;:
    </Box>{' '}
    <Box component="dd" sx={{ display: 'inline', m: 0 }}>
      {children}
    </Box>
  </Typography>
);
