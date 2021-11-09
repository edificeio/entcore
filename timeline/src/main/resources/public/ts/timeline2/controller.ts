import { IIdiom, IUserInfo } from 'ode-ts-client';
import { session, conf } from 'ode-ngjs-front';
import { IController } from 'angular';

export class AppController implements IController {
	me: IUserInfo;
	currentLanguage: string;
	lang: IIdiom;

	// IController implementation
	$onInit(): void {
		this.initialize();
	}

	private async initialize():Promise<void> {
		const platformConf = conf().Platform;
		this.me = session().user;
		this.currentLanguage = session().currentLanguage;
		this.lang = platformConf.idiom;
	}

	public toggleContainer( ev:UIEvent, containerId:string ) {
		$(".list-trigger .trigger").removeClass('on');
		$(ev.currentTarget).addClass('on');
		let classFocus = 'focus-' + containerId;
		$('.container-advanced').removeClass('hide');
		$('.container-advanced').attr('class', 'container-advanced ' + classFocus);
		$('.container-advanced-wrapper').on('transitionend webkitTransitionEnd oTransitionEnd', function () {
			// your event handler
			$('.container-advanced').addClass('hide');
		});
	}

};
