import { Combobox, Dropdown, OptionListItemType } from '@edifice.io/react';
import { Fragment, ReactNode, useEffect, useState } from 'react';
import { useSearchRecipients } from '~/features/message-edit/hooks/useSearchRecipients';
import { useI18n } from '~/hooks';
import { Group, Recipients, User } from '~/models';
import { Visible } from '~/models/visible';
import { useAppActions, useMessageUpdated } from '~/store';
import { RecipientListItem } from './RecipientListItem';
import { RecipientListSelectedItem } from './RecipientListSelectedItem';

export interface RecipientListProps {
  recipients: Recipients;
  head: ReactNode;
  recipientType: 'to' | 'cc' | 'cci';
  onRecipientUpdate?: (recipients: Recipients) => void;
}

export function RecipientListEdit({
  recipients,
  head,
  recipientType,
  onRecipientUpdate,
}: RecipientListProps) {
  const { t } = useI18n();
  const [recipientArray, setRecipientArray] = useState<(User | Group)[]>([]);

  const messageUpdated = useMessageUpdated();
  const { setMessageUpdated, setMessageUpdatedNeedToSave } = useAppActions();

  const handleRecipientClick = (recipient: Visible) => {
    let recipientToAdd: User | Group;
    if (recipient.type === 'User') {
      recipientToAdd = {
        id: recipient.id,
        displayName: recipient.displayName,
        profile: recipient.profile || '',
      };
      recipients.users.push(recipientToAdd);
    } else {
      recipientToAdd = {
        id: recipient.id,
        displayName: recipient.displayName,
        size: recipient.nbUsers || 0,
        subType: recipient.groupType,
      };
      recipients.groups.push(recipientToAdd);
    }
    setRecipientArray((prev) => [...prev, recipientToAdd]);
    updateMessage();
  };

  const handleRemoveRecipient = (recipient: User | Group) => {
    setRecipientArray((prev) =>
      prev.filter((item) => item.id !== recipient.id),
    );
    updateMessage();
  };

  const updateMessage = () => {
    if (!messageUpdated) {
      return;
    }
    messageUpdated[recipientType] = recipients;
    setMessageUpdated({ ...messageUpdated });
    setMessageUpdatedNeedToSave(true);
  };

  const {
    state: { searchResults, searchInputValue, searchAPIResults },
    isSearchLoading,
    hasSearchNoResults,
    searchMinLength,
    handleSearchInputChange,
  } = useSearchRecipients({
    recipientType,
  });

  useEffect(() => {
    onRecipientUpdate?.({
      users: recipientArray.filter(
        (recipient) => 'profile' in recipient && !('size' in recipient),
      ) as User[],
      groups: recipientArray.filter((recipient) => 'size' in recipient),
    });
  }, [onRecipientUpdate, recipientArray]);

  useEffect(() => {
    setRecipientArray([...recipients.users, ...recipients.groups] as (
      | User
      | Group
    )[]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const renderListRecipients = (recipients: OptionListItemType[]) => {
    return recipients.map((recipient, index) => {
      const visible = searchAPIResults.find(
        (visible) => visible.id === recipient.value,
      );
      if (!visible) {
        return null;
      }
      return (
        <Fragment key={index}>
          <RecipientListItem
            onRecipientClick={handleRecipientClick}
            visible={visible}
            disabled={recipientArray.some(
              (recipient) => recipient.id === visible.id,
            )}
          />
        </Fragment>
      );
    });
  };

  return (
    <div className="d-flex align-items-center flex-fill ps-8 pe-16 py-8">
      <Combobox
        value={searchInputValue}
        placeholder={t('conversation.users.search.placeholder')}
        isLoading={isSearchLoading}
        noResult={hasSearchNoResults}
        options={searchResults}
        searchMinLength={searchMinLength}
        onSearchInputChange={handleSearchInputChange}
        variant="ghost"
        renderNoResult={
          <div>
            <h3>
              {t('conversation.users.search.noResult.header', {
                search: searchInputValue,
              })}
            </h3>
            <div>{t('conversation.users.search.noResult.text')}</div>
          </div>
        }
        renderInputGroup={head}
        renderSelectedItems={recipientArray.map((recipient) => {
          const type = 'size' in recipient ? 'group' : 'user';
          return (
            <Fragment key={recipient.id}>
              <RecipientListSelectedItem
                recipient={recipient}
                type={type}
                onRemoveClick={handleRemoveRecipient}
              />
            </Fragment>
          );
        })}
        renderList={(recipients) => (
          <>
            {searchInputValue.length === 0 && !!recipients.length ? (
              <Dropdown.MenuGroup
                label={t('conversation.users.search.favorites')}
              >
                {renderListRecipients(recipients)}
              </Dropdown.MenuGroup>
            ) : (
              renderListRecipients(recipients)
            )}
          </>
        )}
      />
    </div>
  );
}
