import { useI18n } from '~/hooks/useI18n';

export function ReportContent() {
  const { t } = useI18n();
  return (
    <div>
      <h2>{t('reports.detail.tab.content')}</h2>
    </div>
  );
}
