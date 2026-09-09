import { useUserPreferences } from '@edifice.io/react';
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

function getBackgroundImgUrl(background: Background) {
  return backgroundImages[background];
}

export function useBackgroundImage() {
  const { preferences } = useUserPreferences<CustomizationPreferences>();
  const background = (preferences?.background as Background) ?? 'default';

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
