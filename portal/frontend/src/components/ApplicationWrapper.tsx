import { useTranslation } from 'react-i18next';
import { Application } from '~/models/application';
import { ApplicationIcon } from './ApplicationIcon';
import { IconOptions, IconInfoCircle } from '@edifice.io/react/icons';
import clsx from 'clsx';
import { Dropdown, IconButton, IconButtonProps } from '@edifice.io/react';
import { RefAttributes, useState } from 'react';

export function ApplicationWrapper({ data }: { data: Application }) {
  const { t } = useTranslation('common');
  const [dropdownActive, setDropdownActive] = useState(false);
  const [hover, setHover] = useState(false);
  const classApplicationCard = clsx(
    'rounded application-card position-relative py-8 px-4',
    (dropdownActive || hover) && 'active border border-secondary bg-gray-200',
  );
  return (
    <a
      data-id={data.name}
      href={data.address}
      rel={data.isExternal ? 'noopener noreferrer' : undefined}
      target={data.isExternal ? '_blank' : undefined}
      className={classApplicationCard}
      style={{ width: 107, height: 127, textAlign: 'center' }}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
    >
      <ApplicationIcon data={data} />
      <h1
        className="small text-gray-900"
        style={{
          fontFamily: 'Arimo',
          fontWeight: 400,
          textTransform: 'unset',
        }}
      >
        {data.appName}
      </h1>
      <div
        className="dropdown-wrapper"
        style={{ position: 'absolute', top: 0, right: 0 }}
      >
        <Dropdown
          onToggle={(active) => setDropdownActive(active)}
          placement="right-end"
        >
          {(
            triggerProps: JSX.IntrinsicAttributes &
              Omit<IconButtonProps, 'ref'> &
              RefAttributes<HTMLButtonElement>,
          ) => (
            <>
              <IconButton
                {...triggerProps}
                type="button"
                aria-label="label"
                className="bg-secondary-200 border-white text-secondary"
                icon={<IconOptions />}
              />
              <Dropdown.Menu>
                <Dropdown.Item icon={<IconInfoCircle />}>
                  {t('my.apps.open.application')}
                </Dropdown.Item>
                <Dropdown.Item icon={<IconInfoCircle />}>
                  {t('my.apps.add.favorite')}
                </Dropdown.Item>
                <Dropdown.Item icon={<IconInfoCircle />}>
                  {t('my.apps.remove.favorite')}
                </Dropdown.Item>
                <Dropdown.Item icon={<IconInfoCircle />}>
                  {t('my.apps.examples')}
                </Dropdown.Item>
                <Dropdown.Item icon={<IconInfoCircle />}>
                  {t('my.apps.infos')}
                </Dropdown.Item>
              </Dropdown.Menu>
            </>
          )}
        </Dropdown>
      </div>
    </a>
  );
}