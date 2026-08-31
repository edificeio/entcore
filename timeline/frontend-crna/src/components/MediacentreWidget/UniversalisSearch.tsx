import { ButtonBeta, FormControl, Input } from '@edifice.io/react';
import { IconSearch } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import universalisLogo from './assets/universalis.png';

const UNIVERSALIS_SEARCH_URL =
  'https://www.universalis-edu.com/nomade/precherche/';
const UNIVERSALIS_HOME_URL = 'http://www.universalis-edu.com';

interface UniversalisSearchProps {
  /** UAI code of the selected school; the search is disabled without it. */
  uai?: string;
}

/**
 * Same search bar as the lib's Universalis/Qwant widgets: it reuses their
 * markup and their `universalis__search*` classes (lib _universalis.scss).
 */
export function UniversalisSearch({ uai }: UniversalisSearchProps) {
  const { t } = useTranslation('timeline');
  const disabled = !uai;
  const placeholder = t(
    disabled
      ? 'homepage.crna.widget.mediacentre.universalis.no-uai'
      : 'homepage.crna.widget.mediacentre.universalis.search',
    disabled ? 'UAI non renseigné' : 'Rechercher dans Universalis',
  );

  return (
    <div className="mediacentre-universalis-search">
      <a
        href={UNIVERSALIS_HOME_URL}
        target="_blank"
        rel="noopener noreferrer"
        data-testid="mediacentre-universalis-link"
      >
        <img
          src={universalisLogo}
          alt="Universalis"
          className="mediacentre-universalis-search-logo"
        />
      </a>
      <form
        method="GET"
        action={UNIVERSALIS_SEARCH_URL}
        target="_blank"
        autoComplete="off"
        className="universalis__search"
      >
        <FormControl
          id="mediacentre-universalis-search-input"
          className="universalis__search-field"
        >
          <FormControl.Label className="visually-hidden">
            {placeholder}
          </FormControl.Label>
          <Input
            name="q"
            placeholder={placeholder}
            size="md"
            type="text"
            maxLength={255}
            disabled={disabled}
          />
          {/* Required by Universalis' search endpoint, alongside the visible "q" input. */}
          <input type="hidden" name="r" value="www" />
          <input type="hidden" name="uai" value={uai ?? ''} />
          <ButtonBeta
            leftIcon={<IconSearch />}
            type="submit"
            aria-label={placeholder}
            variant="filled"
            color="default"
            disabled={disabled}
            data-testid="mediacentre-universalis-search-button"
          />
        </FormControl>
      </form>
    </div>
  );
}
