import {
  Combobox,
  Dropdown,
  OptionListItemType,
  useIsAdml,
} from '@edifice.io/react';
import { Fragment, ReactNode, useEffect, useState } from 'react';
import { useSearchRecipients } from '~/features/message-edit/hooks/useSearchRecipients';
import { useI18n } from '~/hooks/useI18n';
import { Group, Recipients, User } from '~/models';
import { Visible } from '~/models/visible';
import { useBookmarkById } from '~/services/queries/user';
import { useMessage, useMessageActions } from '~/store/messageStore';
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
  const { isAdml } = useIsAdml();

  const { getBookmarkById } = useBookmarkById();
  const message = useMessage();
  const { setMessage, setMessageNeedToSave } = useMessageActions();
  const [isComboboxFocused, setIsComboboxFocused] = useState(false);

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
        type: recipient.groupType,
        subType: recipient.profile,
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
    if (!message) {
      return;
    }
    message[recipientType] = recipients;
    setMessage({ ...message });
    setMessageNeedToSave(true);
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
    const recipientIds = [
      ...recipients.users.map((user) => user.id),
      ...recipients.groups.map((group) => group.id),
    ];
    // compare with the current recipientArray
    const currentRecipientIds = recipientArray.map((recipient) => recipient.id);
    if (
      recipientIds.length !== currentRecipientIds.length ||
      !recipientIds.every((id) => currentRecipientIds.includes(id))
    ) {
      setRecipientArray([...recipients.users, ...recipients.groups] as (
        | User
        | Group
      )[]);
    }

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [recipients]);

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

  const placeholder =
    isComboboxFocused && isAdml
      ? t('conversation.users.search.placeholder.adml')
      : t('conversation.users.search.placeholder');

  return (
    <div className="d-flex align-items-center flex-fill ps-8 pe-16 py-8">
      <Combobox
        value={searchInputValue}
        placeholder={placeholder}
        isLoading={isSearchLoading}
        noResult={hasSearchNoResults}
        options={searchResults}
        searchMinLength={searchMinLength}
        onSearchInputChange={handleSearchInputChange}
        onSearchInputKeyUp={handleSearchInputKeyUp}
        variant="ghost"
        onFocus={() => setIsComboboxFocused(true)}
        onBlur={() => setIsComboboxFocused(false)}
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
          const type =
            'size' in recipient || 'nbUsers' in recipient ? 'group' : 'user';
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
