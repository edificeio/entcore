import { FormControl, SearchButton } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';
import universalisLogo from './assets/universalis.png';

const UNIVERSALIS_SEARCH_URL =
  'https://www.universalis-edu.com/nomade/precherche/';
const UNIVERSALIS_HOME_URL = 'http://www.universalis-edu.com';

interface UniversalisSearchProps {
  /** UAI code of the selected school; the search is disabled without it. */
  uai?: string;
}

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
      <a href={UNIVERSALIS_HOME_URL} target="_blank" rel="noopener noreferrer">
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
        className="flex-grow-1"
      >
        {/* Required by Universalis' search endpoint, alongside the visible "q" input. */}
        <input type="hidden" name="r" value="www" />
        <input type="hidden" name="uai" value={uai ?? ''} />
        <FormControl
          id="mediacentre-universalis-search-input"
          className="input-group"
        >
          <FormControl.Input
            type="text"
            name="q"
            size="md"
            noValidationIcon
            className="border-end-0"
            placeholder={placeholder}
            aria-label={placeholder}
            disabled={disabled}
            maxLength={255}
          />
          <SearchButton
            type="submit"
            className="border-start-0"
            disabled={disabled}
          />
        </FormControl>
      </form>
    </div>
  );
}
