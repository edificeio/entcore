import '@mui/material/Button';
import '@mui/material/styles';

import { type StateColors } from './states';

declare module '@mui/material/styles' {
  interface Palette {
    tertiary: Palette['primary'];
  }
  interface PaletteOptions {
    tertiary?: PaletteOptions['primary'];
  }
  /** Figma `<color>/_states/*` variables, derived from the main color. */
  interface PaletteColor {
    _states: StateColors;
  }
  interface SimplePaletteColorOptions {
    _states?: StateColors;
  }
  interface TypeText {
    _states: StateColors;
  }
  interface CommonColors {
    _states: { black: StateColors; white: StateColors };
  }
  interface TypeBackground {
    /** Page background behind paper surfaces (Figma background/page). */
    page: string;
  }
  interface TypographyVariants {
    fontWeightSemiBold: number;
  }
  interface TypographyVariantsOptions {
    fontWeightSemiBold?: number;
  }
}

declare module '@mui/material/Button' {
  interface ButtonPropsColorOverrides {
    tertiary: true;
  }
}
