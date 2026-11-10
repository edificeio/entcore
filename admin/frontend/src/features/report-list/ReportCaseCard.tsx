import { Fragment, type ReactNode } from 'react';

import { useDate, useDirectory } from '@edifice.io/react';
import Avatar from '@mui/material/Avatar';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardActions from '@mui/material/CardActions';
import CardContent from '@mui/material/CardContent';
import CardHeader from '@mui/material/CardHeader';
import Divider from '@mui/material/Divider';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import { useI18n } from '~/hooks/useI18n';
import { type Case, type Status } from '~/models';
import { ReportCaseModuleChip } from './ReportCaseModuleChip';
import { ReportCaseStatusChip } from './ReportCaseStatusChip';

/** Shown instead of an empty value, so the row never reads as blank. */
const EMPTY_VALUE = '—';

/** Width of the left column (avatar and content info) in the Figma design. */
const LEFT_COLUMN_WIDTH = 384;

/** Statuses set by a moderation action, each told by a decision line on the card. */
const ACTION_STATUSES: Status[] = [
  'QUARANTINE',
  'RESOURCE_KEPT',
  'RESOURCE_DELETED',
];

/** Lines the reporters list may take before it is cut with an ellipsis. */
const REPORTERS_MAX_LINES = 2;

/** User page of the admin console (Angular app served next to this one). */
const adminUserURL = (structureId: string, userId: string) =>
  `/admin/${structureId}/users/list/${userId}/details`;

export interface ReportCaseCardProps {
  reportCase: Case;
  /** Id of the title, used to label the enclosing feed article. */
  titleId: string;
  /** Structure the cases are moderated for, used to open the author page. */
  structureId: string;
}

interface InfoRowProps {
  label: string;
  /** Cut the value with an ellipsis after this number of lines. */
  maxLines?: number;
  children: ReactNode;
}

/** One "label : value" line, as a term/description pair of the enclosing list. */
const InfoRow = ({ label, maxLines, children }: InfoRowProps) => (
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
      sx={{
        m: 0,
        minWidth: 0,
        overflowWrap: 'anywhere',
        ...(maxLines && {
          display: '-webkit-box',
          WebkitBoxOrient: 'vertical',
          WebkitLineClamp: maxLines,
          overflow: 'hidden',
        }),
      }}
    >
      {children}
    </Typography>
  </Box>
);

/** Card of a reported case in the moderation list. */
export const ReportCaseCard = ({
  reportCase,
  titleId,
  structureId,
}: ReportCaseCardProps) => {
  const { t, common_t } = useI18n();
  const { formatDate } = useDate();
  const { getAvatarURL, getUserbookURL } = useDirectory();

  const reasons = [
    ...new Set(reportCase.reports.map((report) => report.reason)),
  ]
    .map((reason) => t(`reports.reason.${reason}`, { defaultValue: reason }))
    .join(', ');
  // By reporter id: two homonyms are two reporters, and a person reporting twice is one
  const reporters = [
    ...new Map(
      reportCase.reports.map((report) => [
        report.reporterId,
        report.displayName,
      ]),
    ),
  ].filter(([, displayName]) => displayName);
  // Latest decision behind the current status, when that status comes from an action
  const decision = ACTION_STATUSES.includes(reportCase.status)
    ? [...(reportCase.decisions ?? [])]
        .reverse()
        .find(({ status }) => status === reportCase.status)
    : undefined;

  return (
    <Card elevation={4} sx={{ p: 1 }} data-testid="reports-case-card">
      <CardHeader
        title={t('reports.case.title', {
          app: common_t(reportCase.module),
          title: reportCase.title,
        })}
        slotProps={{
          title: { id: titleId, variant: 'h3', component: 'h2' },
          // From sm up, a long title wraps beside the chip instead of pushing it below
          content: { sx: { flexBasis: { xs: 'auto', sm: 0 }, minWidth: 0 } },
          // As in Figma: the chip sits 16px from the card top and right edges
          // and overhangs the header bottom padding instead of growing it
          action: { sx: { alignSelf: 'flex-start', m: 0, mr: -1, mb: -1 } },
        }}
        action={<ReportCaseStatusChip status={reportCase.status} />}
        // The status chip goes under the title on xs screens
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
          {/* Fixed width from md up, so the columns line up from one card to the next */}
          <Stack
            direction="row"
            spacing={3}
            sx={{ flex: { md: `0 0 ${LEFT_COLUMN_WIDTH}px` }, minWidth: 0 }}
          >
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
                  <Link
                    href={adminUserURL(structureId, reportCase.authorId)}
                    data-testid="reports-case-author-link"
                  >
                    {reportCase.authorDisplayName}
                  </Link>
                </InfoRow>
              </Stack>
            </Stack>
          </Stack>

          <Stack
            component="dl"
            spacing={1}
            sx={{ m: 0, flex: 1, minWidth: 0, justifyContent: 'flex-end' }}
          >
            <InfoRow label={t('reports.case.reasons')}>
              {reasons || EMPTY_VALUE}
            </InfoRow>
            <InfoRow label={t('reports.case.date')}>
              {formatDate(reportCase.createdAt, 'L')}
            </InfoRow>
            <InfoRow
              label={t('reports.case.reporters')}
              maxLines={REPORTERS_MAX_LINES}
            >
              {reporters.length > 0
                ? reporters.map(([reporterId, displayName], index) => (
                    <Fragment key={reporterId}>
                      {index > 0 && ', '}
                      <Link
                        href={getUserbookURL(reporterId, 'user')}
                        data-testid="reports-case-reporter-link"
                      >
                        {displayName}
                      </Link>
                    </Fragment>
                  ))
                : EMPTY_VALUE}
            </InfoRow>
          </Stack>
        </Stack>

        {decision && (
          <>
            <Divider sx={{ mt: 2, mb: 1.5 }} />
            <Typography variant="body1" data-testid="reports-case-decision">
              {t(`reports.case.decision.${decision.status}`, {
                date: formatDate(decision.createdAt, 'L'),
                time: formatDate(decision.createdAt, 'LT'),
                name: decision.displayName,
              })}
            </Typography>
          </>
        )}
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
