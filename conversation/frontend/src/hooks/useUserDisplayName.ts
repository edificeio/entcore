import { useEdificeClient } from '@edifice.io/react';
import { useI18n } from '~/hooks/useI18n';
import { Group, User } from '~/models';
import { Visible } from '~/models/visible';

export function useMessageUserDisplayName(entity: User | Group | Visible) {
  const { user } = useEdificeClient();
  const { t } = useI18n();

  return user?.userId === entity.id ? t('me') : entity.displayName;
}
