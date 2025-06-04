import { ApplicationIcon } from "~/components/ApplicationIcon"
import { useApplications } from "~/services"

export function ApplicationList() {
  const { applications } = useApplications()
  console.log(applications)
    return <>
      {applications?.map(app => <ApplicationIcon key={app.name} data={app} />)}
    </>
}