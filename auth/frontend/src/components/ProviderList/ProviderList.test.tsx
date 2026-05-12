import { fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '~/mocks/setup';
import type { WayfProvider } from '~/models/wayf';
import { ProviderList } from '.';

const providers: WayfProvider[] = [
  { i18n: 'wayf.teacher', acs: '/auth/saml/teacher', color: '#c53030' },
  { i18n: 'wayf.student', children: [{ i18n: 'wayf.student', acs: '/auth/saml/student' }] },
];

describe('ProviderList', () => {
  it('renders one button per provider', () => {
    render(<ProviderList providers={providers} onProviderClick={vi.fn()} />);
    expect(screen.getAllByRole('button')).toHaveLength(2);
  });

  it('calls onProviderClick with the clicked provider', () => {
    const handleClick = vi.fn();
    render(<ProviderList providers={providers} onProviderClick={handleClick} />);
    fireEvent.click(screen.getAllByRole('button')[0]);
    expect(handleClick).toHaveBeenCalledWith(providers[0]);
  });
});
