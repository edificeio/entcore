import { useState } from 'react';

import { Options, Plus } from '@edifice-ui/icons';
import { Button, IconButton, useOdeClient } from '@edifice-ui/react';
import { useQueryClient } from '@tanstack/react-query';
import { ActionType } from 'edifice-ts-client';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { baseUrl } from '~/services';
import { ActionBarContainer } from './ActionBarContainer';

export const AppActionBar = () => {
  const { appCode } = useOdeClient();
  const { t } = useTranslation(appCode);
  const { t: common_t } = useTranslation('common');
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  //   const { availableActionsForBlog: availableActions, canContrib } =
  //     useActionDefinitions(blogActions);
  const [isBarOpen, setBarOpen] = useState(false);

  const handleOpenMenuClick = () => {
    setBarOpen((prev) => !prev);
  };

  const handleAddClick = () => {
    navigate(`./post/edit`);
  };

  const handlePrintClick = () => {
    window.open(`${baseUrl}/print`, '_blank');
    setBarOpen(false);
  };

  return (
    <div className="d-flex flex-fill align-items-center justify-content-end gap-12 align-self-end">
      <Button
        leftIcon={<Plus />}
        onClick={handleAddClick}
        className="text-nowrap"
      >
        {t('new.message')}
      </Button>

      <IconButton
        color="primary"
        variant="outline"
        icon={<Options />}
        aria-label={common_t('tiptap.tooltip.plus')}
        onClick={handleOpenMenuClick}
      />

      <ActionBarContainer visible={isBarOpen}>
        <Button
          type="button"
          color="primary"
          variant="filled"
          onClick={handlePrintClick}
        >
          {common_t('explorer.actions.print')}
        </Button>
      </ActionBarContainer>
    </div>
  );
};
