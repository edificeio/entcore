import { WidgetsPersonalizationPanel } from '@edifice.io/react/homepage';
import { useI18n } from '~/hooks/useI18n';
import { useWidgetPreferences } from '~/hooks/useWidgetPreferences';
import { getWidgetIcon } from './widgetIcons';

export interface WidgetsPersonalizationPanelContainerProps {
  onClose: () => void;
}

/**
 * `IWidgetModel.i18n` is a resource path (e.g.
 * "/assets/widgets/school-widget/i18n"), not a translation key — it was
 * never used to resolve a label, even in the legacy AngularJS portal.
 * That older widget picker (the direct predecessor of this panel, in
 * `timeline/src/main/resources/public/template/settings.html`) keyed off
 * `widget.name` instead, under `timeline.settings.<name>` — the same
 * `timeline` i18n namespace already loaded here. Reuse that convention, with
 * a readable name-derived fallback for anything not covered by it.
 */
function fallbackLabel(name: string): string {
  const words = name.replace(/-widget$/, '').split(/[-_]/);
  return words
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

function widgetLabel(
  widget: { name: string },
  t: (key: string, options?: Record<string, unknown>) => string,
): string {
  return t(`timeline.settings.${widget.name}`, {
    defaultValue: fallbackLabel(widget.name),
  });
}

export function WidgetsPersonalizationPanelContainer({
  onClose,
}: WidgetsPersonalizationPanelContainerProps) {
  const { t } = useI18n();
  const { widgets, isLoading, isVisible, isLocked, toggleWidget } =
    useWidgetPreferences();

  const items = widgets.map((widget) => ({
    id: widget.name,
    label: widgetLabel({ name: widget.name }, t),
    icon: getWidgetIcon(widget.name),
    checked: isVisible(widget.name),
    locked: isLocked(widget.name),
  }));

  return (
    <WidgetsPersonalizationPanel
      items={items}
      isLoading={isLoading}
      onToggle={toggleWidget}
      onClose={onClose}
    />
  );
}

WidgetsPersonalizationPanelContainer.displayName =
  'WidgetsPersonalizationPanelContainer';
