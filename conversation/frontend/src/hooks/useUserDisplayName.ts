import { useEdificeClient } from '@edifice.io/react';
import { useI18n } from '~/hooks';
import { Group, User } from '~/models';

export function useMessageUserDisplayName(entity: User | Group) {
  const { user } = useEdificeClient();
  const { t } = useI18n();

  return user?.userId === entity.id ? t('me') : entity.displayName;
}
