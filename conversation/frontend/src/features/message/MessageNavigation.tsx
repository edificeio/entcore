import {
  MessageActionDropDown,
  MessageActionDropDownProps,
} from '~/components/MessageActionDropDown/MessageActionDropDown';
import { MessageProps } from '.';

export function MessageNavigation({ message }: MessageProps) {
  const actionDropDownProps: MessageActionDropDownProps = {
    message,
    appearance: {
      dropdownVariant: 'ghost',
      mainButtonVariant: 'ghost',
      buttonColor: 'tertiary',
    },
  };
  return (
    <nav className="border-bottom px-16 py-4 d-flex">
      <div className="ms-auto">
        <MessageActionDropDown {...actionDropDownProps} />
      </div>
    </nav>
  );
}
