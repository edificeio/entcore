import { alpha, createTheme, type ThemeOptions } from '@mui/material/styles';

import { createStates, withStates } from './states';

// Design tokens come from the Figma "Console DS - Material UI v9.0.0" variables
// (collections palette/light, material/colors, typography, spacing, shape, breakpoints).

/** material/colors greys used by the design system (not MUI's default greys). */
const GREY = {
  50: '#FAFAFA',
  100: '#F7F7F7',
  200: '#EEEEEE',
  300: '#E4E4E4',
  400: '#AAAAAA',
  500: '#909090',
  600: '#757575',
  700: '#696969',
  800: '#424242',
  900: '#212121',
};

/** material/colors blueGrey/800, base of the text colors. */
const TEXT_BASE = '#424852';

const themeOptions: ThemeOptions = {
  // ─── Palette ────────────────────────────────────────────
  palette: {
    mode: 'light',
    primary: withStates({
      light: '#FFA726',
      main: '#FF8D2E',
      dark: '#FF7F14',
      contrastText: '#FFFFFF',
    }),
    secondary: withStates({
      light: '#BA68C8',
      main: '#9C27B0',
      dark: '#7B1FA2',
      contrastText: '#FFFFFF',
    }),
    // Not in the Figma design system: kept for existing usages
    tertiary: withStates({
      main: '#2EC4B6',
      contrastText: '#FFFFFF',
    }),
    error: withStates({
      light: '#EF5350',
      main: '#D32F2F',
      dark: '#C62828',
      contrastText: '#FFFFFF',
    }),
    warning: withStates({
      light: '#FF9800',
      main: '#EF6C00',
      dark: '#E65100',
      contrastText: '#FFFFFF',
    }),
    info: withStates({
      light: '#03A9F4',
      main: '#0288D1',
      dark: '#01579B',
      contrastText: '#FFFFFF',
    }),
    success: withStates({
      light: '#4CAF50',
      main: '#2E7D32',
      dark: '#1B5E20',
      contrastText: '#FFFFFF',
    }),
    grey: GREY,
    text: {
      primary: alpha(TEXT_BASE, 0.87),
      secondary: alpha(TEXT_BASE, 0.6),
      disabled: alpha(TEXT_BASE, 0.38),
      _states: createStates(TEXT_BASE),
    },
    // Figma common/<color>/_states/* → palette.common._states.<color>.*
    common: {
      _states: {
        black: createStates('#000000'),
        white: createStates('#FFFFFF'),
      },
    },
    background: {
      default: '#FFFFFF',
      paper: '#FFFFFF',
      page: '#EAEDF2',
    },
    divider: 'rgba(0, 0, 0, 0.12)',
    action: {
      active: 'rgba(0, 0, 0, 0.54)',
      hover: 'rgba(0, 0, 0, 0.04)',
      hoverOpacity: 0.04,
      selected: 'rgba(0, 0, 0, 0.08)',
      selectedOpacity: 0.08,
      focus: 'rgba(0, 0, 0, 0.12)',
      focusOpacity: 0.12,
      disabled: 'rgba(0, 0, 0, 0.26)',
      disabledBackground: 'rgba(0, 0, 0, 0.12)',
      disabledOpacity: 0.38,
    },
    contrastThreshold: 3,
    tonalOffset: 0.2,
  },

  // ─── Typographie ────────────────────────────────────────
  // Figma text styles (typography/* in the Typography and Button pages of the DS).
  // Letter spacings are in px, as in Figma.
  typography: {
    fontFamily: '"Open Sans", "Roboto", "Helvetica", "Arial", sans-serif',
    fontSize: 14,
    fontWeightLight: 300,
    fontWeightRegular: 400,
    fontWeightMedium: 500,
    fontWeightSemiBold: 600,
    fontWeightBold: 700,
    h1: {
      fontSize: '1.5rem',
      fontWeight: 700,
      lineHeight: 1.167,
      letterSpacing: '-1.5px',
    },
    h2: {
      fontSize: '1.25rem',
      fontWeight: 700,
      lineHeight: 1.2,
      letterSpacing: '-0.5px',
    },
    h3: {
      fontSize: '1.25rem',
      fontWeight: 700,
      lineHeight: 1.167,
      letterSpacing: 0,
    },
    h4: {
      fontSize: '1.125rem',
      fontWeight: 700,
      lineHeight: 1.235,
      letterSpacing: '0.25px',
    },
    h5: {
      fontSize: '1.5rem',
      fontWeight: 400,
      lineHeight: 1.334,
      letterSpacing: 0,
    },
    h6: {
      fontSize: '1.25rem',
      fontWeight: 500,
      lineHeight: 1.6,
      letterSpacing: '0.15px',
    },
    subtitle1: {
      fontSize: '1rem',
      fontWeight: 400,
      lineHeight: 1.75,
      letterSpacing: '0.15px',
    },
    subtitle2: {
      fontSize: '0.875rem',
      fontWeight: 500,
      lineHeight: 1.57,
      letterSpacing: '0.1px',
    },
    body1: {
      fontSize: '1rem',
      fontWeight: 400,
      lineHeight: 1.5,
      letterSpacing: '0.15px',
    },
    body2: {
      fontSize: '0.875rem',
      fontWeight: 400,
      lineHeight: 1.43,
      letterSpacing: '0.17px',
    },
    // typography/buttonMedium; MUI applies buttonSmall/buttonLarge sizes (13px/15px) itself.
    // Figma labels are not uppercased.
    button: {
      fontSize: '0.875rem',
      fontWeight: 500,
      lineHeight: 24 / 14,
      letterSpacing: '0.4px',
      textTransform: 'none',
    },
    caption: {
      fontSize: '0.75rem',
      fontWeight: 400,
      lineHeight: 1.66,
      letterSpacing: '0.4px',
    },
    overline: {
      fontSize: '0.75rem',
      fontWeight: 400,
      lineHeight: 2.66,
      letterSpacing: '1px',
    },
  },

  // ─── Espacement & forme ─────────────────────────────────
  spacing: 8, // theme.spacing(2) = 16px
  shape: {
    borderRadius: 4,
  },

  // ─── Breakpoints ────────────────────────────────────────
  // Figma sets xs to 444 (smallest frame width); MUI needs xs = 0 for mobile-first queries
  breakpoints: {
    values: {
      xs: 0,
      sm: 600,
      md: 900,
      lg: 1200,
      xl: 1536,
    },
  },

  // ─── Transitions ────────────────────────────────────────
  transitions: {
    duration: {
      shortest: 150,
      shorter: 200,
      short: 250,
      standard: 300,
      complex: 375,
      enteringScreen: 225,
      leavingScreen: 195,
    },
    easing: {
      easeInOut: 'cubic-bezier(0.4, 0, 0.2, 1)',
      easeOut: 'cubic-bezier(0.0, 0, 0.2, 1)',
      easeIn: 'cubic-bezier(0.4, 0, 1, 1)',
      sharp: 'cubic-bezier(0.4, 0, 0.6, 1)',
    },
  },

  // ─── Z-index ────────────────────────────────────────────
  zIndex: {
    mobileStepper: 1000,
    fab: 1050,
    speedDial: 1050,
    appBar: 1100,
    drawer: 1200,
    modal: 1300,
    snackbar: 1400,
    tooltip: 1500,
  },

  // ─── Surcharges des composants ──────────────────────────
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          WebkitFontSmoothing: 'antialiased',
          MozOsxFontSmoothing: 'grayscale',
        },
      },
    },
    MuiButton: {
      defaultProps: {
        disableElevation: true,
      },
      styleOverrides: {
        root: {
          padding: '8px 20px',
        },
        sizeSmall: {
          padding: '4px 12px',
        },
        sizeLarge: {
          padding: '12px 28px',
        },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        root: ({ theme }) => ({
          borderRadius: theme.shape.borderRadius,
        }),
      },
    },
    MuiPaper: {
      defaultProps: {
        elevation: 0,
      },
      styleOverrides: {
        root: {
          backgroundImage: 'none', // évite le voile en mode sombre
        },
      },
    },
    // Figma <Tabs> (Console Signalement)
    MuiTabs: {
      styleOverrides: {
        root: ({ theme }) => ({
          backgroundColor: theme.palette.background.default,
          borderTopLeftRadius: theme.shape.borderRadius,
          borderTopRightRadius: theme.shape.borderRadius,
        }),
        indicator: () => ({ height: 5 }),
      },
    },
    MuiTab: {
      styleOverrides: {
        // Visible keyboard focus (RGAA 10.7): the default ripple is too faint
        root: ({ theme }) => ({
          'padding': theme.spacing(2, 4),
          'fontSize': '1rem',
          'lineHeight': '24px',
          'letterSpacing': '0.4px',
          // Full shorthand: styleOverrides don't go through the sx border transform
          'borderBottom': `5px solid ${theme.palette.common._states.black.outlinedBorder}`,
          '&.Mui-selected': {
            fontWeight: theme.typography.fontWeightBold,
            backgroundColor: theme.palette.primary._states.hover,
          },
          '&:not(.Mui-selected)': {
            fontWeight: theme.typography.fontWeightMedium,
            color: theme.palette.common._states.black.outlinedBorder,
          },
          '&.Mui-focusVisible': {
            outline: `2px solid ${theme.palette.primary.main}`,
            outlineOffset: -2,
          },
        }),
      },
    },
    MuiCard: {
      styleOverrides: {
        root: ({ theme }) => ({
          border: `1px solid ${theme.palette.divider}`,
          borderRadius: (theme.shape.borderRadius as number) * 1.5,
        }),
      },
    },
    MuiAppBar: {
      defaultProps: {
        elevation: 0,
        // MUI paints the "default" color with grey[100], the Figma appBar/defaultFill
        color: 'default',
      },
      styleOverrides: {
        root: ({ theme }) => ({
          borderBottom: `1px solid ${theme.palette.divider}`,
        }),
      },
    },
    MuiTextField: {
      defaultProps: {
        variant: 'outlined',
        size: 'small',
        fullWidth: true,
      },
    },
    // Figma input/standard/hoverBorder
    MuiInput: {
      styleOverrides: {
        underline: ({ theme }) => ({
          '&:hover:not(.Mui-disabled, .Mui-error):before': {
            borderBottomColor: theme.palette.common.black,
          },
        }),
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: ({ theme }) => ({
          'backgroundColor': theme.palette.background.paper,
          // Figma input/outlined/hoverBorder
          '&:hover .MuiOutlinedInput-notchedOutline': {
            borderColor: theme.palette.common.black,
          },
          '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
            boxShadow: `0 0 0 3px ${alpha(theme.palette.primary.main, 0.15)}`,
          },
        }),
      },
    },
    MuiChip: {
      styleOverrides: {
        root: ({ theme }) => ({
          // Figma chip/defaultCloseFill, for default chips only:
          // colored chips keep the delete icon derived from their color
          '&.MuiChip-colorDefault .MuiChip-deleteIcon': {
            color: theme.palette.common.black,
          },
          'fontWeight': 500,
        }),
      },
    },
    MuiTableCell: {
      styleOverrides: {
        // Figma table/border (material/colors grey/300)
        root: ({ theme }) => ({
          borderBottomColor: theme.palette.grey[300],
        }),
      },
    },
    MuiSwitch: {
      styleOverrides: {
        // Figma switch/knobFillEnabled; checked and disabled states keep MUI's colors
        switchBase: ({ theme }) => ({
          color: theme.palette.grey[50],
        }),
      },
    },
    MuiRating: {
      styleOverrides: {
        // Figma rating/activeFill
        iconFilled: {
          color: '#FFB400',
        },
        // Figma rating/enabledBorder
        iconEmpty: ({ theme }) => ({
          color: alpha(theme.palette.common.black, 0.23),
        }),
      },
    },
    MuiTooltip: {
      defaultProps: {
        arrow: true,
      },
      styleOverrides: {
        // Figma tooltip/fill: MUI's grey/700, not the design system's grey[700]
        tooltip: {
          backgroundColor: alpha('#616161', 0.9),
          fontSize: '0.75rem',
        },
        arrow: {
          color: alpha('#616161', 0.9),
        },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: ({ theme }) => ({
          borderRadius: (theme.shape.borderRadius as number) * 2,
        }),
      },
    },
    MuiLink: {
      defaultProps: {
        underline: 'hover',
      },
    },
  },
};

const theme = createTheme(themeOptions);

export default theme;
