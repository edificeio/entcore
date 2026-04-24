import { fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '~/mocks/setup';
import type { WayfProvider } from '~/models/wayf';
import { ChildrenList } from '.';

const parent: WayfProvider = {
  i18n: 'student',
  children: [
    { i18n: 'student.primary', acs: '/auth/saml/student-primary' },
    { i18n: 'student.secondary', acs: '/auth/saml/student-secondary' },
  ],
};

describe('ChildrenList', () => {
  it('renders one button per child', () => {
    render(
      <ChildrenList
        parent={parent}
        onBack={vi.fn()}
        onChildClick={vi.fn()}
      />,
    );
    // 2 children + 1 back button
    expect(screen.getAllByRole('button')).toHaveLength(3);
  });

  it('calls onBack when back button is clicked', () => {
    const handleBack = vi.fn();
    render(
      <ChildrenList
        parent={parent}
        onBack={handleBack}
        onChildClick={vi.fn()}
      />,
    );
    fireEvent.click(screen.getAllByRole('button')[0]);
    expect(handleBack).toHaveBeenCalledTimes(1);
  });

  it('calls onChildClick with the selected child', () => {
    const handleChildClick = vi.fn();
    render(
      <ChildrenList
        parent={parent}
        onBack={vi.fn()}
        onChildClick={handleChildClick}
      />,
    );
    // Second button = first child (after back button)
    fireEvent.click(screen.getAllByRole('button')[1]);
    expect(handleChildClick).toHaveBeenCalledWith(parent.children![0]);
  });

  it('propagates parent color key to children buttons', () => {
    render(
      <ChildrenList
        parent={parent}
        onBack={vi.fn()}
        onChildClick={vi.fn()}
      />,
    );
    // Both children should carry the parent's color class (wayf-provider-btn--student)
    const buttons = screen.getAllByRole('button').slice(1); // skip back btn
    buttons.forEach((btn) => {
      expect(btn).toHaveClass('wayf-provider-btn--student');
    });
  });

  it('uses parent.titleI18n for the title when provided', () => {
    const parentWithTitle: WayfProvider = {
      i18n: 'teacher',
      titleI18n: 'wayf.select.academy',
      children: [{ i18n: 'teacher.lille', acs: '/auth/saml/teacher-lille' }],
    };
    render(
      <ChildrenList
        parent={parentWithTitle}
        onBack={vi.fn()}
        onChildClick={vi.fn()}
      />,
    );
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(
      'wayf.select.academy',
    );
  });

  it('renders an empty list when parent has no children', () => {
    const parentNoChildren: WayfProvider = { i18n: 'student' };
    render(
      <ChildrenList
        parent={parentNoChildren}
        onBack={vi.fn()}
        onChildClick={vi.fn()}
      />,
    );
    // Only the back button
    expect(screen.getAllByRole('button')).toHaveLength(1);
  });
});
