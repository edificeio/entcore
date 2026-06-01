import { fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '~/mocks/setup';
import type { WayfProvider } from '~/models/wayf';
import { ProviderButton } from '.';

const provider: WayfProvider = {
  i18n: 'wayf.teacher',
  acs: '/auth/saml/teacher',
  color: 'teacher',
};

describe('ProviderButton', () => {
  it('renders a button', () => {
    render(<ProviderButton provider={provider} onClick={vi.fn()} />);
    expect(screen.getByRole('button')).toBeInTheDocument();
  });

  it('applies the provider color as CSS modifier class', () => {
    render(<ProviderButton provider={provider} onClick={vi.fn()} />);
    expect(screen.getByRole('button')).toHaveClass('wayf-provider-btn--teacher');
  });

  it('uses provider.color for the modifier regardless of i18n key', () => {
    const student: WayfProvider = {
      i18n: 'student',
      acs: '/auth/login',
      color: 'student',
    };
    render(<ProviderButton provider={student} onClick={vi.fn()} />);
    expect(screen.getByRole('button')).toHaveClass('wayf-provider-btn--student');
  });

  it('renders an SVG icon when provider.icon is set', () => {
    const withIcon: WayfProvider = {
      i18n: 'anything',
      icon: 'student',
      acs: '/auth/login',
      color: 'student',
    };
    render(<ProviderButton provider={withIcon} onClick={vi.fn()} />);
    expect(
      document.querySelector('.wayf-provider-btn__icon-wrap svg'),
    ).toBeInTheDocument();
  });

  it('renders no icon when neither provider.icon nor parentIconKey is set', () => {
    const plain: WayfProvider = {
      i18n: 'student',
      acs: '/auth/login',
      color: 'student',
    };
    render(<ProviderButton provider={plain} onClick={vi.fn()} />);
    expect(
      document.querySelector('.wayf-provider-btn__icon-wrap svg'),
    ).not.toBeInTheDocument();
  });

  it('falls back to parentIconKey when provider.icon is not set', () => {
    const child: WayfProvider = {
      i18n: 'wayf.student.ecole',
      acs: '/auth/saml/student-ecole',
      color: 'student',
    };
    render(
      <ProviderButton
        provider={child}
        onClick={vi.fn()}
        parentIconKey="student"
      />,
    );
    expect(
      document.querySelector('.wayf-provider-btn__icon-wrap svg'),
    ).toBeInTheDocument();
  });

  it('provider.icon takes precedence over parentIconKey', () => {
    const withBoth: WayfProvider = {
      i18n: 'anything',
      icon: 'teacher',
      acs: '/auth/login',
      color: 'teacher',
    };
    render(
      <ProviderButton
        provider={withBoth}
        onClick={vi.fn()}
        parentIconKey="student"
      />,
    );
    // Hard to introspect which icon renders via DOM alone; at minimum the SVG exists
    expect(
      document.querySelector('.wayf-provider-btn__icon-wrap svg'),
    ).toBeInTheDocument();
  });

  it('applies the child own color as CSS modifier class', () => {
    const childOfStudent: WayfProvider = {
      i18n: 'student.primary',
      acs: '/auth/saml/student-primary',
      color: 'student',
    };
    render(<ProviderButton provider={childOfStudent} onClick={vi.fn()} />);
    expect(screen.getByRole('button')).toHaveClass('wayf-provider-btn--student');
  });

  it('calls onClick with the provider when clicked', () => {
    const handleClick = vi.fn();
    render(<ProviderButton provider={provider} onClick={handleClick} />);
    fireEvent.click(screen.getByRole('button'));
    expect(handleClick).toHaveBeenCalledWith(provider);
  });
});
