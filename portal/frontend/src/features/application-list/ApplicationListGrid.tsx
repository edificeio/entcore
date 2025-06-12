import { ApplicationWrapper } from "~/components/ApplicationWrapper";
import { Application } from "~/models/application";

export function ApplicationListGrid({ applications }: { applications: Application[] }) {
  return <div
    className="d-flex flex-wrap gap-16 justify-content-center mx-auto"
    style={{ maxWidth: 1091 }}
  >
    {applications.map((application) => (
      <ApplicationWrapper key={application.name} data={application} />
    ))}
  </div>
}