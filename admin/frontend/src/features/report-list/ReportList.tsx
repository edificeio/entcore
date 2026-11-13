import { useI18n } from '~/hooks/useI18n';

export function ReportList() {
  const { t } = useI18n();

  return (
    <div>
      <h2>{t('reports.detail.tab.received')}</h2>
    </div>
  );
}
