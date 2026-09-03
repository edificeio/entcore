import { DYSLEXIC_FONT_ID } from '~/models/customization';
import './CustomizationPreview.css';

/** Number of pictograms outlined on the right of the navigation bar. */
const NAVBAR_ICONS_COUNT = 4;

export type CustomizationPreviewProps = {
  /** `_id` of the font selected in the form (see GET /themes). */
  selecterFontName?: string;
  greetingText: string;
  lastInfosText: string;
};

/**
 * Grey block outlining a homepage content. Purely decorative.
 */
const PreviewBlock = ({ className }: { className: string }) => (
  <div className={`customization-preview-block ${className}`} />
);

/**
 * Miniature preview of the homepage, displayed on the right of the
 * customization form (hidden on small screens, see `customize.tsx`).
 *
 * This is a silhouette: only the greeting and last infos are actual text,
 * everything else is decorative and marked `aria-hidden`. Background color
 * is intentionally hardcoded here — it will be extracted into a prop in a
 * later step.
 */
export const CustomizationPreview = ({
  selecterFontName,
  greetingText,
  lastInfosText,
}: CustomizationPreviewProps) => {
  const fontClassName =
    selecterFontName === DYSLEXIC_FONT_ID ? 'ff-dyslexic' : '';

  return (
    <div className={`customization-preview ${fontClassName}`}>
      <div className="customization-preview-navbar" aria-hidden="true">
        <div className="customization-preview-navbar-logo" />
        <div className="customization-preview-navbar-icons">
          {Array.from({ length: NAVBAR_ICONS_COUNT }, (_, index) => (
            <div key={index} className="customization-preview-navbar-icon" />
          ))}
        </div>
      </div>

      <div className="customization-preview-body">
        <div className="customization-preview-sidebar">
          <PreviewBlock className="customization-preview-block-sidebar-header" />
          <p className="customization-preview-lastinfos">{lastInfosText}</p>
          <PreviewBlock className="customization-preview-block-sidebar-item" />
          <PreviewBlock className="customization-preview-block-sidebar-item" />
        </div>

        <div className="customization-preview-content">
          <div className="customization-preview-card">
            <div className="customization-preview-greeting-header">
              <div
                className="customization-preview-avatar"
                aria-hidden="true"
              />
              <p className="customization-preview-greeting">{greetingText}</p>
            </div>
            <PreviewBlock className="customization-preview-block-greeting" />
          </div>

          <div className="customization-preview-card customization-preview-card-wide">
            <PreviewBlock className="customization-preview-block-label" />
          </div>

          <div className="customization-preview-cards-row">
            <div className="customization-preview-card customization-preview-card-tall">
              <PreviewBlock className="customization-preview-block-label-full" />
            </div>
            <div className="customization-preview-card customization-preview-card-short">
              <PreviewBlock className="customization-preview-block-label-full" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
