import { DisplayActionDropDown } from "~/components/DisplayActionDropDown";
import { MessageProps } from ".";

export function MessageNavigation({ message }: MessageProps) {
  const actionDropDownProps = {
    message,
    appearance: {
      variant: 'ghost' as const,
      btnColor: 'tertiary' as const,
    },
  };
  return (
    <nav className="border-bottom px-16 py-4 d-flex">
      <div className="ms-auto">
        <DisplayActionDropDown {...actionDropDownProps} />
      </div>
    </nav>
  );
}