import { Injectable } from '@angular/core'
import { BundlesService } from 'sijil'
import * as noty from 'noty'
import * as $ from 'jquery'


/* Default options */

// $.noty.defaults.theme = 'defaultTheme'
$.noty.defaults.theme = 'relax'
$.noty.defaults.timeout = 2500
$.noty.defaults.progressBar = true
$.noty.defaults.closeWith = ['button']
$.noty.defaults.template = `
    <div class="noty_message notification">
        <span class="noty_text"></span>
        <div class="noty_close"></div>
    </div>`

export type I18nKey = string | {key: string, parameters: {}};

@Injectable()
export class NotifyService {

    constructor(private bundles : BundlesService){}

    private mixin<T>(_1: T, _2: T) : T {
        if(!_2) return _1
        for(let prop in _2) {
            _1[prop] = _2[prop]
        }
        return _1
    }

    private translate(key: I18nKey) {
        return this.bundles.translate(
                    typeof key === "string" ? key : key.key,
                    typeof key === "object" ? key.parameters : null)
    }

    public notify(
            content: I18nKey,
            title?: I18nKey,
            footer?: I18nKey,
            type?: 'alert' | 'success' | 'error' | 'warning' | 'information' | 'notification',
            opts?: NotyOptions) {

        const titleDiv = title ?
            `<div class="notify-title">
                <strong>
                    ${this.translate(title)}
                </strong>
            </div>` : ''
        const footerDiv = footer ?
            `<div class="notify-footer">
                <span>
                    ${this.translate(footer)}
                </span>
            </div>` : ''

        const options : NotyOptions = {
            text: `${titleDiv}
                   <div class="notify-content">
                        <span>
                            ${this.translate(content)}
                        </span>
                    </div>
                    ${footerDiv}`,
            type: type || 'information'
        }
        noty(this.mixin(options, opts))
    }

    public success(content: I18nKey, title?: I18nKey, opts?: NotyOptions) {
        this.notify(content, title, null, 'success', opts)
    }

    public info(content: I18nKey, title?: I18nKey, opts?: NotyOptions) {
        this.notify(content, title, null, 'information', opts)
    }

    public error(content: I18nKey, title? : I18nKey, err?, opts?: NotyOptions) {
        const errorMessage = err ? 
            (err && err.response && err.response.data && err.response.data.error || err.message)
            : undefined;
        this.notify(content, title, errorMessage, 'error', this.mixin({
            timeout: false
        }, opts))
    }
}
