import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { saveThemePreference } from '~/services/api/theme.api';
import {
  currentThemeQueryOptions,
  themesQueryOptions,
} from '~/services/queries/theme.queries';

export function useThemePreference() {
  const [currentSkin, setCurrentSkin] = useState('default');

  const { data: themeList = [] } = useQuery(themesQueryOptions());
  const { data: currentTheme } = useQuery(currentThemeQueryOptions());

  useEffect(() => {
    const skin = currentTheme?.skinName ?? 'default';
    setCurrentSkin(skin);
    document.documentElement.setAttribute('data-skin', skin);
  }, [currentTheme?.skinName]);

  const setTheme = async (skinId: string) => {
    setCurrentSkin(skinId);
    document.documentElement.setAttribute('data-skin', skinId);
    if (currentTheme?.themeName) {
      await saveThemePreference(currentTheme.themeName, skinId);
    }
  };

  return { themes: themeList, currentSkin, setTheme };
}
