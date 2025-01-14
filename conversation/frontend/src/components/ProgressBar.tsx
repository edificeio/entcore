import { clsx } from 'clsx';

export type ProgressBarProps = {
  /** Label to display. */
  label: string;
  /** Options to customize the label. */
  labelOptions?: {
    /**
     * Force the position of the label within the bar container.
     * When `undefined`, the label is centered in the progress, not container.
     */
    justify?: 'start' | 'center' | 'end';
    /**
     * Allow the label to overflow the progress bar, bleeding inside the container.
     * Otherwise, the label is clipped to the progress.
     * Default to `false`, but forced to `true` when `justify` is defined.
     */
    overflow?: boolean;
  };
  /** Progress to display, in percent from 0 to 100. */
  progress: number;
  progressOptions?: {
    /** Keyword defining the color of the bar. */
    color?: 'info' | 'warning' | 'danger';
    /** Style of bar. */
    fill?: 'plain' | 'stripes' | 'animated-stripes';
  };
};

export function ProgressBar({
  label,
  labelOptions,
  progressOptions,
  ...props
}: ProgressBarProps) {
  const progress = Math.round(props.progress);

  let overflow = false;
  if (labelOptions) {
    if (labelOptions.overflow === true || labelOptions.justify) {
      overflow = true;
    }
  }

  let color: 'info' | 'warning' | 'danger' = 'info';
  if (progressOptions) {
    if (progressOptions.color) {
      color = progressOptions.color;
    }
  }

  const barClassName = clsx('progress-bar text-gray-800', {
    'overflow-visible': overflow,
    'bg-warning': color === 'warning',
    'bg-danger': color === 'danger',
  });

  return (
    <div
      className="progress border"
      role="progressbar"
      aria-label="Success example"
      aria-valuenow={25}
      aria-valuemin={0}
      aria-valuemax={100}
      style={{ height: '20px' }}
    >
      <div className={barClassName} style={{ width: `${progress}%` }}>
        {label}
      </div>
    </div>
  );
}
