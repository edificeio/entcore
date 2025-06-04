import { Application } from "~/models/application";

export function ApplicationIcon({data} : {data: Application}) {
    return <div>
        {data.displayName}
    </div>
}