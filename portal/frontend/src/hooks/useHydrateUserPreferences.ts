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
    if (preferences && !isHydrated) {
      setPreferences(preferences);
    }
  }, [preferences, isHydrated, setPreferences]);

  return {
    isHydrated
  }
};
