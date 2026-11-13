import { useDate, useDirectory } from '@edifice.io/react';
import Avatar from '@mui/material/Avatar';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Divider from '@mui/material/Divider';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import { type Theme } from '@mui/material/styles';
import Typography from '@mui/material/Typography';
import useMediaQuery from '@mui/material/useMediaQuery';

import { EMPTY_VALUE, InfoRow } from '~/components/InfoRow';
import { useI18n } from '~/hooks/useI18n';
import { type Report } from '~/models';

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
  // The columns sit side by side from md up, one under the other below
  const isWide = useMediaQuery((theme: Theme) => theme.breakpoints.up('md'));

  return (
    <Card variant="outlined" data-testid="reports-report-card">
      <CardContent sx={{ 'p': 2, '&:last-child': { pb: 2 } }}>
        <Stack
          direction={{ xs: 'column', md: 'row' }}
          spacing={2}
          divider={
            <Divider
              orientation={isWide ? 'vertical' : 'horizontal'}
              flexItem
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
                data-testid="reports-report-reporter"
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

          {/* Unlike the info rows, the comment starts on the line after its label */}
          <Box component="dl" sx={{ m: 0, flex: 1, minWidth: 0 }}>
            <Typography
              component="dt"
              variant="body1"
              sx={{ fontWeight: 'fontWeightBold' }}
            >
              {t('reports.report.comment')}&nbsp;:
            </Typography>
            <Typography
              component="dd"
              variant="body1"
              // Keep the line breaks the reporter typed, and wrap long words such as URLs
              sx={{ m: 0, whiteSpace: 'pre-line', overflowWrap: 'anywhere' }}
              data-testid="reports-report-comment"
            >
              {report.comment || EMPTY_VALUE}
            </Typography>
          </Box>
        </Stack>
      </CardContent>
    </Card>
  );
};
