import { useQuery } from "@tanstack/react-query";
import { useEffect } from "react";
import { preferencesService } from "~/services";
import { useUserPreferencesStore } from "~/store/userPreferencesStore";

export const useHydrateUserPreferences = () => {
  const { data: preferences } = useQuery({
    queryKey: ['user-preferences'],
    queryFn: preferencesService.getUserPreferences,
  });

  const { isHydrated, setPreferences } = useUserPreferencesStore();

  useEffect(() => {
    if (!isHydrated && preferences !== undefined) {
      const parsed = preferences ?? { applications: [], bookmarks: [] };
      setPreferences(parsed);
    }
  }, [preferences, isHydrated, setPreferences]);

  return {
    isHydrated
  }
};
