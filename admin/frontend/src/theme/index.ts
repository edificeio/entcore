import { alpha, createTheme, type ThemeOptions } from '@mui/material/styles';

const themeOptions: ThemeOptions = {
  // ─── Palette ────────────────────────────────────────────
  palette: {
    mode: 'light',
    primary: {
      light: '#8A7FF5',
      main: '#5B4CF0',
      dark: '#3F33C2',
      contrastText: '#FFFFFF',
    },
    secondary: {
      light: '#FF9A9A',
      main: '#FF6B6B',
      dark: '#D94848',
      contrastText: '#FFFFFF',
    },
    tertiary: {
      main: '#2EC4B6',
      contrastText: '#FFFFFF',
    },
    error: {
      main: '#E5484D',
    },
    warning: {
      main: '#F5A524',
    },
    info: {
      main: '#3E8EF7',
    },
    success: {
      main: '#30A46C',
    },
    grey: {
      50: '#FAFAFC',
      100: '#F2F2F7',
      200: '#E4E4EC',
      300: '#D1D1DB',
      400: '#A1A1AF',
      500: '#71717F',
      600: '#52525E',
      700: '#3F3F49',
      800: '#27272F',
      900: '#18181D',
    },
    text: {
      primary: '#18181D',
      secondary: '#52525E',
      disabled: '#A1A1AF',
    },
    background: {
      default: '#F7F7FB',
      paper: '#FFFFFF',
    },
    divider: '#E4E4EC',
    action: {
      hover: 'rgba(91, 76, 240, 0.06)',
      selected: 'rgba(91, 76, 240, 0.12)',
      disabledOpacity: 0.38,
    },
    contrastThreshold: 3,
    tonalOffset: 0.2,
  },

  // ─── Typographie ────────────────────────────────────────
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    fontSize: 14,
    fontWeightLight: 300,
    fontWeightRegular: 400,
    fontWeightMedium: 500,
    fontWeightBold: 700,
    h1: { fontSize: '2.5rem', fontWeight: 700, lineHeight: 1.2 },
    h2: { fontSize: '2rem', fontWeight: 700, lineHeight: 1.25 },
    h3: { fontSize: '1.75rem', fontWeight: 600, lineHeight: 1.3 },
    h4: { fontSize: '1.5rem', fontWeight: 600, lineHeight: 1.35 },
    h5: { fontSize: '1.25rem', fontWeight: 600, lineHeight: 1.4 },
    h6: { fontSize: '1.125rem', fontWeight: 600, lineHeight: 1.4 },
    subtitle1: { fontSize: '1rem', fontWeight: 500 },
    subtitle2: { fontSize: '0.875rem', fontWeight: 500 },
    body1: { fontSize: '1rem', lineHeight: 1.6 },
    body2: { fontSize: '0.875rem', lineHeight: 1.55 },
    button: { textTransform: 'none', fontWeight: 600 },
    caption: { fontSize: '0.75rem' },
    overline: { fontSize: '0.75rem', fontWeight: 600, letterSpacing: '0.08em' },
  },

  // ─── Espacement & forme ─────────────────────────────────
  spacing: 8, // theme.spacing(2) = 16px
  shape: {
    borderRadius: 10,
  },

  // ─── Breakpoints ────────────────────────────────────────
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
        color: 'inherit',
      },
      styleOverrides: {
        root: ({ theme }) => ({
          borderBottom: `1px solid ${theme.palette.divider}`,
          backgroundColor: theme.palette.background.paper,
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
    MuiOutlinedInput: {
      styleOverrides: {
        root: ({ theme }) => ({
          'backgroundColor': theme.palette.background.paper,
          '&:hover .MuiOutlinedInput-notchedOutline': {
            borderColor: theme.palette.primary.light,
          },
          '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
            boxShadow: `0 0 0 3px ${alpha(theme.palette.primary.main, 0.15)}`,
          },
        }),
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          fontWeight: 500,
        },
      },
    },
    MuiTooltip: {
      defaultProps: {
        arrow: true,
      },
      styleOverrides: {
        tooltip: ({ theme }) => ({
          backgroundColor: theme.palette.grey[900],
          fontSize: '0.75rem',
        }),
        arrow: ({ theme }) => ({
          color: theme.palette.grey[900],
        }),
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
