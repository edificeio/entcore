import { clsx } from 'clsx';
import './ProgressBar.css';

type BarColor = 'info' | 'warning' | 'danger';
type BarFill = 'plain' | 'stripes' | 'animated-stripes';

export type ProgressBarProps = {
  /** Label to display. */
  label: string;
  /** Options to customize the label. */
  labelOptions?: {
    /**
     * Force the position of the label within the bar *container*.
     * When `undefined`, the label is centered inside the *progress bar*, not the container.
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
    color?: BarColor;
    /** Style of bar. */
    fill?: BarFill;
  };
};

export function ProgressBar({
  label,
  labelOptions,
  progressOptions,
  ...props
}: ProgressBarProps) {
  const progress = Math.min(100, Math.max(0, Math.round(props.progress)));

  let overflow = false;
  if (labelOptions?.overflow === true || labelOptions?.justify) {
    overflow = true;
  }

  let color: BarColor = 'info',
    fill: BarFill = 'plain';
  if (progressOptions?.color) {
    color = progressOptions.color;
  }
  if (progressOptions?.fill) {
    fill = progressOptions.fill;
  }

  const barClassName = clsx('progress-bar', {
    'overflow-visible': overflow,
    'bg-secondary-300': color === 'info',
    'bg-orange-300': color === 'warning',
    'bg-red-300': color === 'danger',
    'progress-bar-striped': fill === 'stripes' || fill === 'animated-stripes',
    'progress-bar-animated': fill === 'animated-stripes',
    'w-100': typeof labelOptions?.justify === 'string',
  });

  return (
    <div
      className="progress border"
      role="progressbar"
      aria-label="Success example"
      aria-valuenow={25}
      aria-valuemin={0}
      aria-valuemax={100}
    >
      {labelOptions?.justify ? (
        <div
          className={barClassName}
          style={{
            background: `linear-gradient(to right, transparent ${progress}%, white ${100 - progress}%)`,
          }}
        >
          <div
            className={clsx(
              'label text-gray-800 mx-8',
              labelOptions?.justify && 'align-self-' + labelOptions.justify,
            )}
          >
            {label}
          </div>
        </div>
      ) : (
        <div className={barClassName} style={{ width: `${progress}%` }}>
          <div className="label text-gray-800">{label}</div>
        </div>
      )}
    </div>
  );
}
