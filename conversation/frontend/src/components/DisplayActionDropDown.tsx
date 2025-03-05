import { Dropdown, IconButton, IconButtonProps, Button } from "@edifice.io/react";
import { IconDelete, IconOptions, IconPrint, IconRedo, IconRestore, IconSend, IconUndo, IconUndoAll } from "@edifice.io/react/icons";
import { RefAttributes } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useConfirmModalStore } from "~/hooks/useConfirmModalStore";
import { Message } from "~/models";
import { useDeleteMessage } from "~/services";

export function DisplayActionDropDown({message}: {message: Message}) {
    const { t } = useTranslation('conversation');
    const navigate = useNavigate();
    const openModal = useConfirmModalStore((state) => state.openModal);
    const deleteMessage = useDeleteMessage();

    const handleDelete = () => {
      openModal({
        id: "delete-modal",
        header: <>{t('delete.definitely')}</>,
        body: <p>{t('delete.definitely.confirm')}</p>,
        okText: t('confirm'),
        koText: t('cancel'),
        onSuccess: () => {
          deleteMessage.mutate({ id: message.id });
          navigate('/trash');
        },
      });
    }

    const buttonAction = [
      {
        label: t("reply"),
        id: "reply",
        icon: <IconUndo />,
        action: () => {
          alert('reply');
        },
        hidden: message.state === 'DRAFT' || message.trashed
      },
      {
        label: t("submit"),
        id: "submit",
        icon: <IconSend />,
        action: () => {
          alert('submit');
        },
        hidden: message.state !== 'DRAFT' || message.trashed
      },
      {
        label: t("restore"),
        id: "restore",
        icon: <IconRestore />,
        action: () => {
          alert('restore');
        },
        hidden: !message.trashed
      },
    ];

    const options = [
        {
          label: t("replyall"),
          icon: <IconUndoAll />,
          action: () => {
            alert('reply all');
          },
          hidden: message.state === 'DRAFT' || message.trashed
        },
        {
          label: t("transfer"),
          icon: <IconRedo />,
          action: () => {
            alert('transfer');
          },
          hidden: message.state === 'DRAFT' || message.trashed
        },
        {
            label: t("trash"),
            icon: <IconDelete />,
            action: () => {
              alert('delete');
            },
            hidden: message.trashed
        },
        {
          label: t("delete"),
          icon: <IconDelete />,
          action: handleDelete,
          hidden: !message.trashed
        },
        {
            label: t("print"),
            icon: <IconPrint />,
            action: () => {
                alert('print');
            },
            hidden: message.state === 'DRAFT'
          },
      ];

    return <>
      {buttonAction.filter(o => !o.hidden).map((option) => (
        <Button
            key={option.id}
            color="primary"
            variant="outline"
            leftIcon={option.icon}
            onClick={option.action}
          >
            {option.label}
        </Button>
        ))
      }
      <Dropdown>
      {(
      triggerProps: JSX.IntrinsicAttributes &
        Omit<IconButtonProps, 'ref'> &
        RefAttributes<HTMLButtonElement>,
      ) => (
      <div data-testid="dropdown">
        <IconButton
          {...triggerProps}
          type="button"
          size="sm"
          aria-label=""
          color="primary"
          variant="outline"
          icon={<IconOptions />}
        />
        <Dropdown.Menu>
          {options.filter(o => !o.hidden).map((option) => (
            <Dropdown.Item
              key={option.label}
              icon={option.icon}
              onClick={() => option.action()}
            >
              {option.label}
            </Dropdown.Item>
          ))}
        </Dropdown.Menu>
      </div>
      )}
      </Dropdown>
  </>
}