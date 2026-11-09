import { type ComponentType, type SVGProps } from 'react';

import { IconMail } from '@edifice.io/react/icons';
import {
  IconActualites,
  IconBlog,
  IconWiki,
} from '@edifice.io/react/icons/apps';
import Chip from '@mui/material/Chip';

import { useI18n } from '~/hooks/useI18n';

/** Icon of the modules that can report content; other modules get no icon. */
const MODULE_ICONS: Record<string, ComponentType<SVGProps<SVGSVGElement>>> = {
  conversation: IconMail,
  blog: IconBlog,
  news: IconActualites,
  wiki: IconWiki,
};

/** Figma material/colors orange/50 and orange/500, not part of the theme palette. */
const MODULE_CHIP_BACKGROUND = '#FFF3E0';
const MODULE_CHIP_COLOR = '#FF9800';

export interface ReportCaseModuleChipProps {
  module: string;
}

/** Application the reported content comes from. */
export const ReportCaseModuleChip = ({ module }: ReportCaseModuleChipProps) => {
  const { common_t } = useI18n();
  const Icon = MODULE_ICONS[module];

  return (
    <Chip
      variant="outlined"
      icon={Icon ? <Icon width={20} height={20} /> : undefined}
      // Application names live in the platform common i18n bundle
      label={common_t(module)}
      data-testid="reports-case-module"
      sx={(theme) => ({
        'bgcolor': MODULE_CHIP_BACKGROUND,
        'color': MODULE_CHIP_COLOR,
        'borderColor': theme.palette.primary._states.outlinedBorder,
        ...theme.typography.body2,
        'fontWeight': theme.typography.fontWeightBold,
        '& .MuiChip-icon': { color: 'inherit' },
      })}
    />
  );
};
