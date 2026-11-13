import { configure } from '@testing-library/react';

// The first render of a test file waits for EdificeClientProvider to initialise
// and for cold module transforms: the 1s default made findBy* queries flaky,
// and 3s was nearly used up locally. Keep it below the Vitest testTimeout.
configure({ asyncUtilTimeout: 5000 });
