import '@mui/material/Button';
import '@mui/material/styles';

declare module '@mui/material/styles' {
  interface Palette {
    tertiary: Palette['primary'];
  }
  interface PaletteOptions {
    tertiary?: PaletteOptions['primary'];
  }
}

declare module '@mui/material/Button' {
  interface ButtonPropsColorOverrides {
    tertiary: true;
  }
}
