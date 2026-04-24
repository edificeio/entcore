import { IconArrowLeft } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import type { WayfProvider } from '~/models/wayf';
import { ProviderList } from '../ProviderList';
import './ChildrenList.css';

interface ChildrenListProps {
  parent: WayfProvider;
  onBack: () => void;
  onChildClick: (child: WayfProvider) => void;
}

export const ChildrenList = ({
  parent,
  onBack,
  onChildClick,
}: ChildrenListProps) => {
  const { t } = useTranslation('auth');

  const children = parent.children ?? [];
  const parentKey = parent.i18n.replace(/wayf\./, '');

  // Config validation: a level-2 child must not itself have children.
  if (import.meta.env.DEV) {
    children.forEach((child) => {
      if (child.children?.length) {
        console.warn(
          `[WAYF] Invalid config: child "${child.i18n}" of "${parent.i18n}" has its own children. Only 2 levels are supported — nested children are ignored.`,
        );
      }
      if (!child.acs) {
        console.warn(
          `[WAYF] Invalid config: child "${child.i18n}" of "${parent.i18n}" has no acs. Children must be terminal.`,
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
        providers={children}
        onProviderClick={onChildClick}
        parentColorKey={parentKey}
        parentIconKey={parent.icon}
      />
    </div>
  );
};
