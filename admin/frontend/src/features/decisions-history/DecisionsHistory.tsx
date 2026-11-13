import { useI18n } from '~/hooks/useI18n';

export function DecisionsHistory() {
  const { t } = useI18n();
  return (
    <div>
      <h2>{t('reports.detail.tab.history')}</h2>
    </div>
  );
}
