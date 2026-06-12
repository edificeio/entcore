import { ChangeDetectorRef, Component, Input } from '@angular/core';
import { NotifyService } from 'src/app/core/services/notify.service';
import { globalStore } from 'src/app/core/store/global.store';
import { DEFAULT_AUTH_CONFIG } from 'src/app/core/store/models/structure.model';
import { Profile } from 'src/app/imports-exports/import/user.model';
import { AuthConfig, AuthModeService, DefaultAuthConfig } from './auth-mode.service';

@Component({
  selector: 'ode-structure-auth-mode',
  templateUrl: './auth-mode.component.html',
  styleUrls: ['./auth-mode.component.scss'],
})
export class AuthModeComponent {
  private _structureId: string;

  @Input() set structureId(id: string) {
    this._structureId = id;
    if (id) this.getAuthConfig(id);
  }
  @Input() isADMC: boolean = false;

  authConfig: DefaultAuthConfig | null = null;
  authModes: AuthConfig = { ...DEFAULT_AUTH_CONFIG.defaultAuthModes };
  showConfirmModal: boolean = false;
  isSaving: boolean = false;
  readonly AUTH_PROFILES: Profile[] = ['Student', 'Teacher', 'Relative', 'Personnel', 'Guest'];

  constructor(
    private authModeService: AuthModeService,
    private notify: NotifyService,
    private changeDetector: ChangeDetectorRef,
  ) {}

  get isAuthConfigValid(): boolean {
    return this.AUTH_PROFILES.every(p => !!this.authModes[p]);
  }

  get isAuthConfigDirty(): boolean {
    const config = this.authConfig;
    if (!config) return false;
    return this.AUTH_PROFILES.some(p => this.authModes[p] !== config.defaultAuthModes[p]);
  }

  saveAuthConfig(): void {
    this.isSaving = true;
    this.authModeService.updateDefaultAuthConfig(this._structureId, { defaultAuthModes: this.authModes }).subscribe({
      next: () => {
        this.authConfig = { defaultAuthModes: { ...this.authModes } };
        globalStore.structures.get(this._structureId)?.syncAuthMode(true);
        this.notify.success('management.structure.informations.authMode.notify.success');
        this.isSaving = false;
        this.changeDetector.markForCheck();
        this.showConfirmModal = false;
      },
      error: () => {
        this.notify.error('management.structure.informations.authMode.notify.error');
        this.isSaving = false;
        this.changeDetector.markForCheck();
      }
    });
  }

  private getAuthConfig(structureId: string): void {
    const config = globalStore.structures.get(structureId)?.getAuthConfig() ?? null;
    this.authConfig = config;
    this.authModes = { ...( config ?? DEFAULT_AUTH_CONFIG).defaultAuthModes };
    this.changeDetector.markForCheck();
  }
}
