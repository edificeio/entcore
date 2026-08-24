import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  fetchCurrentThemeName,
  saveThemePreference,
} from '~/services/api/theme.api';
import { themesQueryOptions } from '~/services/queries/theme.queries';

export function useFontPreference() {
  const [currentTheme, setCurrentTheme] = useState('default');
  const [themeName, setThemeName] = useState('na');

  const { data: themes = [] } = useQuery(themesQueryOptions());
  const sortedThemes = [...themes].sort((a, b) => a._id.localeCompare(b._id));

  useEffect(() => {
    fetchCurrentThemeName().then((name) => {
      if (name.themeName) setThemeName(name.themeName);
      if (name.skinName) setCurrentTheme(name.skinName);
    });
  }, []);

  const setTheme = async (themeId: string) => {
    setCurrentTheme(themeId);
    await saveThemePreference(themeName, themeId);
  };

  return { themes: sortedThemes, currentTheme, setTheme };
}
