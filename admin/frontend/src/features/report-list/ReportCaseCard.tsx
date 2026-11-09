import { type ReactNode } from 'react';

import { useDate, useDirectory } from '@edifice.io/react';
import Avatar from '@mui/material/Avatar';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardActions from '@mui/material/CardActions';
import CardContent from '@mui/material/CardContent';
import CardHeader from '@mui/material/CardHeader';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import { useI18n } from '~/hooks/useI18n';
import { type Case } from '~/models';
import { ReportCaseModuleChip } from './ReportCaseModuleChip';
import { ReportCaseStatusChip } from './ReportCaseStatusChip';

export interface ReportCaseCardProps {
  reportCase: Case;
  /** Id of the title, used to label the enclosing feed article. */
  titleId: string;
}

interface InfoRowProps {
  label: string;
  children: ReactNode;
}

/** One "label : value" line, as a term/description pair of the enclosing list. */
const InfoRow = ({ label, children }: InfoRowProps) => (
  // Wraps the value under its label on narrow screens
  <Box
    sx={{
      display: 'flex',
      flexWrap: 'wrap',
      columnGap: 0.5,
      alignItems: 'baseline',
    }}
  >
    <Typography
      component="dt"
      variant="body1"
      sx={{
        fontWeight: 'fontWeightBold',
        whiteSpace: { xs: 'normal', sm: 'nowrap' },
      }}
    >
      {label}&nbsp;:
    </Typography>
    <Typography
      component="dd"
      variant="body1"
      sx={{ m: 0, minWidth: 0, overflowWrap: 'anywhere' }}
    >
      {children}
    </Typography>
  </Box>
);

/** Card of a reported case in the moderation list. */
export const ReportCaseCard = ({
  reportCase,
  titleId,
}: ReportCaseCardProps) => {
  const { t } = useI18n();
  const { formatDate } = useDate();
  const { getAvatarURL } = useDirectory();

  const reasons = [
    ...new Set(reportCase.reports.map((report) => report.reason)),
  ];
  const reporters = [
    ...new Set(reportCase.reports.map((report) => report.displayName)),
  ];

  return (
    <Card elevation={4} sx={{ p: 1 }} data-testid="reports-case-card">
      <CardHeader
        title={reportCase.title}
        slotProps={{
          title: { id: titleId, variant: 'h3', component: 'h2' },
          action: { sx: { alignSelf: 'center', m: 0 } },
        }}
        action={<ReportCaseStatusChip status={reportCase.status} />}
        // The status chip goes under the title on narrow screens
        sx={{ flexWrap: 'wrap', rowGap: 1 }}
      />

      <CardContent sx={{ px: 2, pt: 1, pb: 2 }}>
        <Divider sx={{ mb: 2 }} />

        <Stack
          direction={{ xs: 'column', md: 'row' }}
          spacing={3}
          divider={
            <Divider
              orientation="vertical"
              flexItem
              sx={{ display: { xs: 'none', md: 'block' } }}
            />
          }
        >
          <Stack direction="row" spacing={3}>
            <Avatar
              src={getAvatarURL(reportCase.authorId, 'user')}
              alt=""
              sx={{ width: 64, height: 64 }}
            />
            <Stack spacing={1.5} sx={{ alignItems: 'flex-start', minWidth: 0 }}>
              <ReportCaseModuleChip module={reportCase.module} />
              <Stack component="dl" spacing={1} sx={{ m: 0 }}>
                <InfoRow label={t('reports.case.content.type')}>
                  {t(`reports.case.type.${reportCase.type}`, {
                    defaultValue: reportCase.type,
                  })}
                </InfoRow>
                <InfoRow label={t('reports.case.content.title')}>
                  {reportCase.title}
                </InfoRow>
                <InfoRow label={t('reports.case.author')}>
                  {reportCase.authorDisplayName}
                </InfoRow>
              </Stack>
            </Stack>
          </Stack>

          <Stack
            component="dl"
            spacing={1}
            sx={{ m: 0, justifyContent: 'flex-end' }}
          >
            <InfoRow label={t('reports.case.reasons')}>
              {reasons
                .map((reason) => t(`reports.reason.${reason}`))
                .join(', ')}
            </InfoRow>
            <InfoRow label={t('reports.case.date')}>
              {formatDate(reportCase.createdAt, 'L')}
            </InfoRow>
            <InfoRow label={t('reports.case.reporters')}>
              {reporters.join(', ')}
            </InfoRow>
          </Stack>
        </Stack>
      </CardContent>

      <CardActions sx={{ justifyContent: 'flex-end', p: 1 }}>
        {/* TODO: open the case detail once its page exists */}
        <Button
          variant="contained"
          // Same label on every card: the case title tells them apart
          aria-describedby={titleId}
          data-testid="reports-case-consult-button"
        >
          {t('reports.case.consult')}
        </Button>
      </CardActions>
    </Card>
  );
};
