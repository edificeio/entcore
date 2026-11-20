import Box from '@mui/material/Box';
import Radio from '@mui/material/Radio';
import RadioGroup from '@mui/material/RadioGroup';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import { useI18n } from '~/hooks/useI18n';
import { type ProcessOption } from './processOptions';

type DecisionStatus = ProcessOption['status'];

export interface DecisionOptionsProps {
  options: ProcessOption[];
  /** Chosen decision; none at first. */
  value: DecisionStatus | null;
  onChange: (status: DecisionStatus) => void;
  /** Id of the "Décision" heading, used as the radio group label. */
  labelledBy: string;
}

/** Decisions available on a case, each one as a bordered card (Figma). */
export function DecisionOptions({
  options,
  value,
  onChange,
  labelledBy,
}: DecisionOptionsProps) {
  const { t } = useI18n();

  return (
    <RadioGroup
      aria-labelledby={labelledBy}
      // An empty value keeps every radio unchecked
      value={value ?? ''}
      onChange={(event) => onChange(event.target.value as DecisionStatus)}
    >
      <Stack spacing={1.5}>
        {options.map(({ status }) => {
          const titleId = `report-case-process-option-${status}-title`;
          const descriptionId = `report-case-process-option-${status}-description`;
          return (
            // The whole card is the label, so a click anywhere on it chooses it
            <Box
              key={status}
              component="label"
              sx={{
                display: 'block',
                px: 1,
                py: 1,
                border: 1,
                borderColor: 'grey.500',
                borderRadius: 1,
                cursor: 'pointer',
              }}
              data-testid={`report-case-process-option-${status}`}
            >
              <Stack direction="row" sx={{ alignItems: 'center' }}>
                <Radio
                  value={status}
                  size="small"
                  sx={{ p: 0, mr: 1 }}
                  slotProps={{
                    input: {
                      // Named by its title only; the description is announced after it
                      'aria-labelledby': titleId,
                      'aria-describedby': descriptionId,
                    },
                  }}
                />
                <Typography
                  id={titleId}
                  variant="body2"
                  sx={{ fontWeight: 'fontWeightBold' }}
                >
                  {t(`reports.process.option.${status}`)}
                </Typography>
              </Stack>
              <Typography id={descriptionId} variant="body2" sx={{ mt: 0.5 }}>
                {t(`reports.process.option.${status}.description`)}
              </Typography>
            </Box>
          );
        })}
      </Stack>
    </RadioGroup>
  );
}
