import { Combobox, Dropdown, OptionListItemType } from '@edifice.io/react';
import { Fragment, ReactNode, useEffect, useState } from 'react';
import { useSearchRecipients } from '~/features/message-edit/hooks/useSearchRecipients';
import { useI18n } from '~/hooks';
import { Group, Recipients, User } from '~/models';
import { Visible } from '~/models/visible';
import { useBookmarkById } from '~/services/queries/user';
import { useAppActions, useMessageUpdated } from '~/store';
import { RecipientListItem } from './RecipientListItem';
import { RecipientListSelectedItem } from './RecipientListSelectedItem';

export type RecipientType = 'to' | 'cc' | 'cci';
export interface RecipientListProps {
  recipients: Recipients;
  head: ReactNode;
  recipientType: RecipientType;
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

  const { getBookmarkById } = useBookmarkById();
  const messageUpdated = useMessageUpdated();
  const { setMessageUpdated, setMessageUpdatedNeedToSave } = useAppActions();

  const handleRecipientClick = async (recipient: Visible) => {
    let recipientToAdd: User | Group;
    if (recipient.type === 'User') {
      recipientToAdd = {
        id: recipient.id,
        displayName: recipient.displayName,
        profile: recipient.profile || '',
      };
      recipients.users.push(recipientToAdd);
      setRecipientArray((prev) => [...prev, recipientToAdd]);
    } else if (recipient.type !== 'ShareBookmark') {
      recipientToAdd = {
        id: recipient.id,
        displayName: recipient.displayName,
        size: recipient.nbUsers || 0,
        subType: recipient.type,
      };
      recipients.groups.push(recipientToAdd);
      setRecipientArray((prev) => [...prev, recipientToAdd]);
    } else {
      const shareBookmark = await getBookmarkById(recipient.id);

      if (shareBookmark) {
        setRecipientArray((prev) => {
          const newRecipients = [
            ...prev,
            ...shareBookmark.users,
            ...shareBookmark.groups,
          ];
          // Remove duplicates by id
          return [...new Set(newRecipients.map((item) => item.id))].map(
            (id) => newRecipients.find((item) => item.id === id)!,
          );
        });
        recipients.users.push(
          ...shareBookmark.users.map((user) => ({
            id: user.id,
            displayName: user.displayName,
            profile: user.profile || '',
          })),
        );
        recipients.groups.push(
          ...shareBookmark.groups.map((group) => ({
            id: group.id,
            displayName: group.displayName,
          })),
        );
      }
    }
    updateMessage();
  };

  const handleRemoveRecipient = (recipient: User | Group) => {
    setRecipientArray((prev) =>
      prev.filter((item) => item.id !== recipient.id),
    );
    recipients.users = recipients.users.filter(
      (item) => item.id !== recipient.id,
    );
    recipients.groups = recipients.groups.filter(
      (item) => item.id !== recipient.id,
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
    handleSearchInputKeyUp,
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
      const isSelected = recipientArray.some(
        (recipient) => recipient.id === visible.id,
      );
      return (
        <Fragment key={index}>
          <RecipientListItem
            onRecipientClick={handleRecipientClick}
            visible={visible}
            recipientType={recipientType}
            disabled={isSelected || recipient.disabled}
            isSelected={isSelected}
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
        onSearchInputKeyUp={handleSearchInputKeyUp}
        variant="ghost"
        renderNoResult={
          <div className="p-8">
            <h4>
              {t('conversation.users.search.noResult.header', {
                search: searchInputValue,
              })}
            </h4>
            <div>{t('conversation.users.search.noResult.text')}</div>
          </div>
        }
        renderInputGroup={head}
        renderSelectedItems={recipientArray.map((recipient) => {
          const type = 'size' in recipient ? 'group' : 'user';
          return (
            <li key={recipient.id} className="d-inline">
              <RecipientListSelectedItem
                recipient={recipient}
                type={type}
                onRemoveClick={handleRemoveRecipient}
              />
            </li>
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
