import { queryOptions } from "@tanstack/react-query";
import { fetchCarnetDeBord } from "../api/carnetDeBord.api";

export const carnetDeBordQueryOptions = queryOptions({
  queryKey: ["carnet-de-bord"],
  queryFn: fetchCarnetDeBord,
  staleTime: 5 * 60 * 1000,
});
