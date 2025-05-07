import { IconButton } from '@edifice.io/react';
import { IconRafterLeft, IconRafterRight } from '@edifice.io/react/icons';
import { useI18n } from '~/hooks';

interface PaginationProps {
  onChange: (page: number) => void;
  /**
   * The current page number starting from 1
   */
  current: number;
  total: number;
}
export default function Pagination({
  onChange,
  current,
  total,
}: PaginationProps) {
  const { t } = useI18n();

  const canGoNext = current < total;
  const canGoPrevious = current > 1;

  const handlePrevious = () => {
    if (canGoPrevious) {
      onChange(current - 1);
    }
  };

  const handleNext = () => {
    if (canGoNext) {
      onChange(current + 1);
    }
  };
  return (
    <div className="d-flex align-items-center gap-4">
      <IconButton
        color="tertiary"
        variant="ghost"
        icon={<IconRafterLeft />}
        onClick={handlePrevious}
        disabled={!canGoPrevious}
      />
      <span>{t('pagination.on', { current, total })}</span>
      <IconButton
        color="tertiary"
        variant="ghost"
        icon={<IconRafterRight />}
        onClick={handleNext}
        disabled={!canGoNext}
      />
    </div>
  );
}
