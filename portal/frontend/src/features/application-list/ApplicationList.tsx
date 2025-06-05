import { ApplicationIcon } from "~/components/ApplicationIcon"
import { useApplications } from "~/services"

export function ApplicationList() {
  const { applications } = useApplications();
    return (
      <div
        className="d-flex flex-wrap gap-16 justify-content-center mx-auto"
        style={{ maxWidth: 1091 }}
      >
        {applications?.map((app) => (
          <ApplicationIcon key={app.name} data={app} />
        ))}
      </div>
    );
}