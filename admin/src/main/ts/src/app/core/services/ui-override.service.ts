import { Injectable } from '@angular/core';
import { http } from 'entcore-toolkit';
import { SessionModel } from '../store/models/session.model';

type UiOverride = string | { variant?: string };

type ThemeConf = {
    overriding: Array<{
        child: string;
        uiOverrides?: { [key: string]: UiOverride };
    }>;
};

/**
 * Same uiOverrides mechanism as @edifice.io/react's useUiOverride and infra-front's skin.ts:
 * a platform's /assets/theme-conf.js can set `overriding[].uiOverrides[key]` to
 * `'variant'` or `{ variant: 'variant' }` to opt into a UI variant, per skin.
 */
@Injectable()
export class UiOverrideService {
    private themeConf: Promise<ThemeConf | undefined>;

    public async getUiOverride(key: string): Promise<string | undefined> {
        const [conf, skin] = await Promise.all([this.getThemeConf(), this.getSkin()]);
        const currentTheme = conf?.overriding?.find(t => t.child === skin);
        const raw = currentTheme?.uiOverrides?.[key];
        const override = typeof raw === 'string' ? { variant: raw } : raw;
        return override?.variant;
    }

    // Skin name from the theme path, e.g. "/assets/themes/<skin>/skins/default/"
    private async getSkin(): Promise<string | undefined> {
        const theme = await SessionModel.getTheme();
        const path = theme?.skin?.split('/assets/themes/')[1];
        return path?.split('/')[0];
    }

    // theme-conf.js is a CommonJS script (`exports.conf = {...}`), not JSON
    private getThemeConf(): Promise<ThemeConf | undefined> {
        if (!this.themeConf) {
            this.themeConf = http.get('/assets/theme-conf.js', { responseType: 'text' })
                .then(response => {
                    const exports: { conf?: ThemeConf } = {};
                    new Function('exports', '"use strict";' + response.data)(exports);
                    return exports.conf;
                })
                .catch(e => {
                    console.error(e);
                    return undefined;
                });
        }
        return this.themeConf;
    }
}
