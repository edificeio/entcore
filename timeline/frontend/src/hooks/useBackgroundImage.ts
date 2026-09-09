import { useUiOverride, useUserPreferences } from '@edifice.io/react';
import { useCallback } from 'react';
import { Background } from '~/services';
import { CustomizationPreferences } from './useCustomization';

// Background images are shipped by @edifice.io/bootstrap (aliased as @images).
const backgroundImages = Object.entries(
  import.meta.glob('@images/backgrounds/*.png', {
    eager: true,
    import: 'default',
    query: '?url',
  }),
).reduce<Record<string, string>>((acc, [path, url]) => {
  const name = path
    .split('/')
    .pop()!
    .replace(/\.png$/, '');
  acc[name] = url as string;
  return acc;
}, {});

export function useBackgroundImage() {
  const customBackgroundUrl = useUiOverride('layout.defaultBackgroundUrl');
  const { preferences } = useUserPreferences<CustomizationPreferences>();
  const background = (preferences?.background as Background) ?? 'default';

  const getBackgroundImgUrl = useCallback(
    (background: Background) => {
      if (customBackgroundUrl && background === 'default')
        return customBackgroundUrl.variant;
      return backgroundImages[background];
    },
    [customBackgroundUrl],
  );

  return {
    background,
    getBackgroundImgUrl,
    backgroundImgStyle: {
      backgroundImage: `url(${getBackgroundImgUrl(background)})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat',
    },
  };
}
