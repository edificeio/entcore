import { useDate, useDirectory } from '@edifice.io/react';
import Avatar from '@mui/material/Avatar';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Divider from '@mui/material/Divider';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import { InfoRow } from '~/components/InfoRow';
import { useI18n } from '~/hooks/useI18n';
import { type Report } from '~/models';

/** Shown instead of an empty value, so the row never reads as blank. */
const EMPTY_VALUE = '—';

/** Width of the left column (reporter and report info) in the Figma design. */
const LEFT_COLUMN_WIDTH = 315;

export interface ReportCardProps {
  report: Report;
}

/** Card of one report received on a case: who reported it, when, why and their comment. */
export const ReportCard = ({ report }: ReportCardProps) => {
  const { t } = useI18n();
  const { formatDate } = useDate();
  const { getAvatarURL, getUserbookURL } = useDirectory();

  return (
    <Card variant="outlined" data-testid="reports-report-card">
      <CardContent sx={{ 'p': 2, '&:last-child': { pb: 2 } }}>
        <Stack
          direction={{ xs: 'column', md: 'row' }}
          spacing={2}
          divider={
            <Divider
              orientation="vertical"
              flexItem
              // Vertical line between the columns from md up, horizontal one below
              sx={{
                borderRightWidth: { xs: 0, md: 'thin' },
                borderBottomWidth: { xs: 'thin', md: 0 },
              }}
            />
          }
        >
          {/* Fixed width from md up, so the columns line up from one card to the next */}
          <Stack
            spacing={1.5}
            sx={{ flex: { md: `0 0 ${LEFT_COLUMN_WIDTH}px` }, minWidth: 0 }}
          >
            <Stack
              direction="row"
              spacing={2}
              sx={{ alignItems: 'flex-start' }}
            >
              <Avatar
                src={getAvatarURL(report.reporterId, 'user')}
                alt=""
                sx={{ width: 64, height: 64 }}
              />
              <Typography
                variant="body1"
                sx={{ minWidth: 0, overflowWrap: 'anywhere' }}
              >
                {report.displayName ? (
                  <Link
                    href={getUserbookURL(report.reporterId, 'user')}
                    data-testid="reports-report-reporter-link"
                  >
                    {report.displayName}
                  </Link>
                ) : (
                  EMPTY_VALUE
                )}
              </Typography>
            </Stack>
            <Box component="dl" sx={{ m: 0 }}>
              <InfoRow label={t('reports.report.date')}>
                {t('reports.report.datetime', {
                  date: formatDate(report.createdAt, 'L'),
                  time: formatDate(report.createdAt, 'LT'),
                })}
              </InfoRow>
              <InfoRow label={t('reports.report.reason')}>
                {t(`reports.reason.${report.reason}`, {
                  defaultValue: report.reason,
                })}
              </InfoRow>
            </Box>
          </Stack>

          <Box component="dl" sx={{ m: 0, flex: 1, minWidth: 0 }}>
            <InfoRow label={t('reports.report.comment')}>
              {/* Keep the line breaks the reporter typed */}
              <Box
                component="span"
                sx={{ whiteSpace: 'pre-line' }}
                data-testid="reports-report-comment"
              >
                {report.comment || EMPTY_VALUE}
              </Box>
            </InfoRow>
          </Box>
        </Stack>
      </CardContent>
    </Card>
  );
};
