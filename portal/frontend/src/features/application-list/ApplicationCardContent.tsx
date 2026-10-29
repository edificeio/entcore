import { ApplicationIcon } from '~/components/ApplicationIcon/ApplicationIcon';
import { Application } from '~/models/application';

export function ApplicationCardContent({
  application,
}: {
  application: Application;
}) {
  return (
    <>
      <ApplicationIcon data={application} isFavorite />
      <h1
        className="small text-gray-900 ellipsis-3 application-title"
        title={application.appName}
      >
        {application.appName}
      </h1>
    </>
  );
}
