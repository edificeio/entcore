import { IconButton } from '@edifice.io/react';
import { IconNotification } from '@edifice.io/react/icons';
import { OnboardingModal, OnboardingModalRef } from '@edifice.io/react/modals';
import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import illuOnboardingIncoming from '~/assets/illu-onboarding-incoming.svg';
import illuOnboardingNew from '~/assets/illu-onboarding-new.svg';

export default function MyAppOnboardingModal() {
  const { t } = useTranslation();

  const onboardingModalRef = useRef<OnboardingModalRef>(null);
  const [isOnboarding, setIsOnboarding] = useState(false);

  return (
    <>
      <div style={{ position: 'relative', display: 'inline-block' }}>
        <IconButton
          aria-label={t('my.app.notification')}
          className="bg-secondary-200 fw-bold"
          color="black"
          icon={<IconNotification />}
          type="button"
          variant="filled"
          onClick={() => {
            onboardingModalRef.current?.setIsOpen(true);
          }}
        />
        {isOnboarding && (
          <span
            style={{
              position: 'absolute',
              top: 0,
              right: 0,
              width: 15,
              height: 15,
              background: 'red',
              borderRadius: '50%',
              border: '2px solid white',
              pointerEvents: 'none',
              zIndex: 1,
              display: 'block',
            }}
            aria-label={t('my.app.onboarding.badge')}
          />
        )}
      </div>
      <OnboardingModal
        ref={onboardingModalRef}
        id="showOnboardingMyApps52"
        items={[
          {
            src: illuOnboardingNew,
            title: 'my.apps.onboarding.modal.screen1.title',
            alt: 'my.apps.onboarding.modal.screen1.alt',
            text: 'my.apps.onboarding.modal.screen1.text',
          },
          {
            src: illuOnboardingIncoming,
            title: 'my.apps.onboarding.modal.screen2.title',
            alt: 'my.apps.onboarding.modal.screen2.alt',
            text: 'my.apps.onboarding.modal.screen2.text',
          },
          {
            src: illuOnboardingNew,
            title: 'my.apps.onboarding.modal.screen3.title',
            alt: 'my.apps.onboarding.modal.screen3.alt',
            text: 'my.apps.onboarding.modal.screen3.text',
          },
        ]}
        isOnboardingChange={(isOnboarding) => {
          setIsOnboarding(isOnboarding);
        }}
      />
    </>
  );
}
