import { alpha } from '@mui/material/styles';

/** Opacities of the Figma `_states` variables, shared by every color. */
const STATE_OPACITIES = {
  hover: 0.04,
  selected: 0.08,
  focus: 0.12,
  focusVisible: 0.3,
  outlinedBorder: 0.5,
} as const;

export type StateColors = Record<keyof typeof STATE_OPACITIES, string>;

/**
 * Builds the Figma `<color>/_states/*` variables from a base color,
 * so they follow the base color instead of being copied by hand.
 */
export function createStates(color: string): StateColors {
  return {
    hover: alpha(color, STATE_OPACITIES.hover),
    selected: alpha(color, STATE_OPACITIES.selected),
    focus: alpha(color, STATE_OPACITIES.focus),
    focusVisible: alpha(color, STATE_OPACITIES.focusVisible),
    outlinedBorder: alpha(color, STATE_OPACITIES.outlinedBorder),
  };
}

/** Adds the `_states` variables of a palette color, derived from its main color. */
export function withStates<T extends { main: string }>(
  color: T,
): T & { _states: StateColors } {
  return { ...color, _states: createStates(color.main) };
}
