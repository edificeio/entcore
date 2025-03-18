import { DisplayActionDropDown } from "~/components/DisplayActionDropDown";
import { MessageProps } from ".";

export function MessageNavigation({ message }: MessageProps) {
  return <nav className="border-bottom mb-16 px-16 py-4 d-flex">
    <div className="ms-auto">
      <DisplayActionDropDown message={message} variant="ghost" />
    </div>
  </nav>
}