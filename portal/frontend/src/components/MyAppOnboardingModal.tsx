import { IconButton } from '@edifice.io/react';
import { IconNotification } from '@edifice.io/react/icons';
import { OnboardingModal, OnboardingModalRef } from '@edifice.io/react/modals';
import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import illuOnboardingActu from '~/assets/illu-onboarding-actu.svg';

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
          icon={<IconNotification color="black" />}
          type="button"
          variant="ghost"
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
        id="showOnboardingMyAppsNewsActu"
        items={[
          {
            src: illuOnboardingActu,
            title: 'my.apps.onboarding.modal.actu.title',
            alt: 'my.apps.onboarding.modal.actu.alt',
            text: 'my.apps.onboarding.modal.actu.text',
          },
        ]}
        isOnboardingChange={(isOnboarding) => {
          setIsOnboarding(isOnboarding);
        }}
      />
    </>
  );
}
