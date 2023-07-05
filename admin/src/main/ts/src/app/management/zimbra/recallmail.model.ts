import { ActionStatus } from './enum/action-status.enum';
interface IMessage {
    mailId: string;
    subject: string;
    mail_date: number;
}

interface IAction {
    approved: boolean,
    date: number,
    tasks: ITask,
    userId: string,

}

interface ITask {
    finished: number;
    error: number;
    total: number;
    lastUpdate: number;
}

export class RecallMail {
    recallMailId: number;
    userName: string;
    comment: string;
    message: IMessage;
    statutDisplayed?: string;
    status: string;
    action: IAction;

    static getActionStatus(action: IAction): ActionStatus {
        let tasks: ITask = action.tasks;
        if (!action.approved) {
            return ActionStatus.WAITING;
        }
        if (tasks.finished === tasks.total) {
            return ActionStatus.REMOVED;
        } else if (tasks.error === 0) {
            return ActionStatus.PROGRESS;
        } else {
            return ActionStatus.ERROR;
        }
    }
}
