import { Dropdown, IconButton, IconButtonProps } from '@edifice.io/react';
import { Fragment, RefAttributes } from 'react';

export type ProgressBarProps = {
  label: string;
  labelPlacement?: 'start' | 'center' | 'end';
  className?: string;
  bars: {
    progress: number;
    style?: 'plain' | 'stripes' | 'animated-stripes';
    className?: string;
  }[];
};

export function ProgressBar(props: ProgressBarProps) {
  return (
    <div
      className="progress"
      role="progressbar"
      aria-label="Success example"
      aria-valuenow="25"
      aria-valuemin="0"
      aria-valuemax="100"
    >
      <div className="progress-bar bg-success" style="width: 25%">
        25%
      </div>
    </div>
  );
}
