import { IconArrowLeft } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import type { WayfParentProvider, WayfProvider } from '~/models/wayf';
import { ProviderList } from '../ProviderList';
import './ChildrenList.css';

interface ChildrenListProps {
  parent: WayfParentProvider;
  onBack: () => void;
  onChildClick: (child: WayfProvider) => void;
}

export const ChildrenList = ({
  parent,
  onBack,
  onChildClick,
}: ChildrenListProps) => {
  const { t } = useTranslation('auth');

  // Config validation: a level-2 child must not itself have children.
  if (import.meta.env.DEV) {
    parent.children.forEach((child) => {
      if ('children' in child) {
        console.warn(
          `[WAYF] Invalid config: child "${child.i18n}" of "${parent.i18n}" has its own children. Only 2 levels are supported — nested children are ignored.`,
        );
      }
    });
  }

  return (
    <div className="wayf-children-list">
      <button type="button" className="wayf-back-btn" onClick={onBack}>
        <IconArrowLeft aria-hidden="true" width={20} height={20} />
        <span>{t('wayf.link.back') || 'Retour'}</span>
      </button>

      <h1 className="wayf-title">
        {t(parent.titleI18n ?? 'wayf.select.level')}
      </h1>

      <ProviderList
        providers={parent.children}
        onProviderClick={onChildClick}
        parentIconKey={parent.icon}
      />
    </div>
  );
};
