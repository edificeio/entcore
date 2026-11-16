import { useId, useState, type ReactNode } from 'react';

import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Collapse from '@mui/material/Collapse';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';

export interface CollapsibleCardProps {
  /** Always visible part, on the left of the toggle. */
  header: ReactNode;
  /** Toggle label while collapsed. */
  expandLabel: string;
  /** Toggle label while expanded. */
  collapseLabel: string;
  /** Whether the card starts expanded, collapsed by default. */
  defaultExpanded?: boolean;
  /** Content shown once expanded, under a separator. */
  children: ReactNode;
  /** Set on the card; the toggle gets `<id>-toggle` and the panel `<id>-panel`. */
  'data-testid'?: string;
}

/** Outlined card whose content is shown or hidden by a text button. */
export const CollapsibleCard = ({
  header,
  expandLabel,
  collapseLabel,
  defaultExpanded = false,
  children,
  'data-testid': testId,
}: CollapsibleCardProps) => {
  const [expanded, setExpanded] = useState(defaultExpanded);
  const panelId = useId();

  return (
    <Card variant="outlined" data-testid={testId}>
      <CardContent sx={{ 'p': 1.5, '&:last-child': { pb: 1.5 } }}>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1}
          sx={{
            justifyContent: 'space-between',
            alignItems: { xs: 'flex-start', sm: 'center' },
          }}
        >
          <Box sx={{ minWidth: 0 }}>{header}</Box>
          {/* Native button: Enter and Space toggle it without extra code */}
          <Button
            type="button"
            variant="text"
            aria-expanded={expanded}
            aria-controls={panelId}
            onClick={() => setExpanded((value) => !value)}
            endIcon={
              <ExpandMoreIcon
                aria-hidden
                sx={{
                  transform: expanded ? 'rotate(180deg)' : 'none',
                  transition: 'transform 150ms',
                }}
              />
            }
            data-testid={testId && `${testId}-toggle`}
            sx={{ flexShrink: 0 }}
          >
            {expanded ? collapseLabel : expandLabel}
          </Button>
        </Stack>
        <Collapse in={expanded} unmountOnExit>
          <Box id={panelId} data-testid={testId && `${testId}-panel`}>
            <Divider sx={{ my: 2 }} />
            {children}
          </Box>
        </Collapse>
      </CardContent>
    </Card>
  );
};
