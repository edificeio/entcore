export interface AlerteParams {
    uri: string;
    bookingUri: string;
    username: string;
    startdate: string;
    enddate: string;
    resourcename: string;
    resourceUri: string;
    date: any;
    reportedStructures: [];
    reporters: [];
    created: any;
    message: string;
}

export class AlerteModel {
    _id: string;
    type: string;
    // event-type: 'PERIODIC-BOOKING-CREATED',
    resource: string;
    sender: string;
    params: AlerteParams;
    date: any;
    reportedStructures: string[];
    reporters: any[];
    created: any;
    message: string;
    reportAction: any;
}
