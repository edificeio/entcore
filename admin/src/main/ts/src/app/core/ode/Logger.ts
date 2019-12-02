import { Injectable, isDevMode } from '@angular/core';

@Injectable({
    providedIn: 'root'
})
export class Logger {

    debug(message: string, context: any, object?: any): void   {
        if (isDevMode()) {
            const logEntry = this.createLogStatement( message, context);
            console.log(`%c<${new Date().toLocaleString()}> %c[${context.constructor.name}] %c${'         ' + message || ''} %c`,
             'color:#398f3f',
             'color:#58c75f',
             'color:#6ee676',
             'color:#87f58e',
             object || '');
        }
    }

    error(message: string, context: any, object?: any): void {
        if (isDevMode()) {
            const logEntry = this.createLogStatement(message, context);
            console.error('         ' + logEntry, object || '');
        }
    }

    warn(message: string, context: any, object?: any): void  {
        if (isDevMode()) {
            const logEntry = this.createLogStatement( message, context);
            console.warn('         ' + logEntry, object || '');
        }
    }

    info(message: string, context: any, object?: any): void {
        if (isDevMode()) {
            const logEntry = this.createLogStatement(message, context);
            console.log(`%c<${new Date().toLocaleString()}> %c[${context.constructor.name}] %c${'         ' + message || ''} %c`,
             'color:#3293a8',
             'color:#09c3eb',
             'color:#78dede',
             'color:#78aede',
             object || '');
        }
    }

    createLogStatement(message,  context: any) {
        return `<${new Date().toLocaleString()}> [${context.constructor.name}] ${'         ' + message || ''} `;
    }

    constructor() {
        if (isDevMode()) {
            let text = '%c  ___                     ____  _       _ _        _ \n' +
            ' / _ \\ _ __   ___ _ __   |  _ \\(_) __ _(_) |_ __ _| |\n' +
            '| | | | ._ \\ / _ \\ ._ \\  | | | | |/ _` | | __/ _` | |\n' +
            '| |_| | |_) |  __/ | | | | |_| | | (_| | | || (_| | |\n' +
            ' \\___/| .__/ \\___|_| |_| |____/|_|\\__, |_|\\__\\__,_|_|\n' +
            ' _____|_| _                 _   _ |___/              \n' +
            '| ____|__| |_   _  ___ __ _| |_(_) ___  _ __         \n' +
            '|  _| / _. | | | |/ __/ _. | __| |/ _ \\| ._ \\        \n' +
            '| |__| (_| | |_| | (_| (_| | |_| | (_) | | | |       \n' +
            '|_____\\__,_|\\__,_|\\___\\__,_|\\__|_|\\___/|_| |_|       \n';
            console.log(text, 'color:#a83299');
        }
    }
}
