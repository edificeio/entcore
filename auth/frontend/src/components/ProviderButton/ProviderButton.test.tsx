import { fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '~/mocks/setup';
import type { WayfProvider } from '~/models/wayf';
import { ProviderButton } from '.';

const provider: WayfProvider = {
  i18n: 'wayf.teacher',
  acs: '/auth/saml/teacher',
  color: '#c53030',
};

describe('ProviderButton', () => {
  it('renders a button', () => {
    render(<ProviderButton provider={provider} onClick={vi.fn()} />);
    expect(screen.getByRole('button')).toBeInTheDocument();
  });

  it('strips wayf. prefix for CSS modifier class', () => {
    render(<ProviderButton provider={provider} onClick={vi.fn()} />);
    expect(screen.getByRole('button')).toHaveClass('wayf-provider-btn--teacher');
  });

  it('uses bare key as CSS modifier when no wayf. prefix', () => {
    const student: WayfProvider = { i18n: 'student', acs: '/auth/login' };
    render(<ProviderButton provider={student} onClick={vi.fn()} />);
    expect(screen.getByRole('button')).toHaveClass('wayf-provider-btn--student');
  });

  it('renders an SVG icon for a known i18n key', () => {
    const student: WayfProvider = { i18n: 'student', acs: '/auth/login' };
    render(<ProviderButton provider={student} onClick={vi.fn()} />);
    expect(document.querySelector('.wayf-provider-btn__icon-wrap svg')).toBeInTheDocument();
  });

  it('renders no icon for an unknown i18n key', () => {
    const custom: WayfProvider = { i18n: 'custom.unknown', acs: '/auth/login' };
    render(<ProviderButton provider={custom} onClick={vi.fn()} />);
    expect(document.querySelector('.wayf-provider-btn__icon-wrap svg')).not.toBeInTheDocument();
  });

  it('calls onClick with the provider when clicked', () => {
    const handleClick = vi.fn();
    render(<ProviderButton provider={provider} onClick={handleClick} />);
    fireEvent.click(screen.getByRole('button'));
    expect(handleClick).toHaveBeenCalledWith(provider);
  });
});
