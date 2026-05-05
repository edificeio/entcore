import {
  Grid,
  LoadingScreen,
  PageLayout,
  useEdificeClient,
} from '@edifice.io/react';
import { MessageFlashList, SchoolSpace } from '@edifice.io/react/homepage';
import { useState } from 'react';
import backgroundImage from '~/assets/background.png';
import { AppFooter } from '~/components/AppFooter';
import { BetaSwitch } from '~/components/BetaSwitch/BetaSwitch';
import { WidgetCard } from '~/components/WidgetCard';
import { WelcomeWidget } from '~/components/WelcomeWidget';
import { LiensUtilesWidget } from '~/components/LiensUtilesWidget';
import {
  MOCK_AVANTAGES,
  MOCK_EMPLOI_DU_TEMPS,
  MOCK_LIENS_UTILES,
  MOCK_MES_EMPRUNTS,
  MOCK_VIE_SCOLAIRE,
} from '~/mocks/widgetsMockData';
import { AvantagesWidget } from '~/components/AvantagesWidget/AvantagesWidget';
import { VieScolaireWidget } from '~/components/VieScolaireWidget/VieScolaireWidget';
import { EmploiDuTempsWidget } from '~/components/EmploiDuTempsWidget';
import { MesEmpruntsWidget } from '~/components/MesEmpruntsWidget/MesEmpruntsWidget';

const MOCK_SCHOOLS = [
  {
    id: 'school-1',
    name: 'Collège Jean Moulin',
    UAI: '0012345A',
    classes: [],
    exports: [],
  },
  {
    id: 'school-2',
    name: 'Lycée Jeanne Ferry de Loisette en Royan',
    UAI: '0098765Z',
    classes: [],
    exports: [],
  },
];

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Root = () => {
  const { init } = useEdificeClient();
  const [selectedSchool, setSelectedSchool] = useState(MOCK_SCHOOLS[1]);

  if (!init) return <LoadingScreen position={false} />;

  return (
    <div data-product="edifice2d">
      <PageLayout
        variant="fullpage"
        style={{
          backgroundImage: `url(${backgroundImage})`,
          backgroundSize: 'cover',
          backgroundPosition: 'center',
          backgroundRepeat: 'no-repeat',
        }}
        scrollMode="columns"
      >
        <BetaSwitch />
        <PageLayout.Header />
        <PageLayout.SidebarLeft style={{ backgroundColor: '#FFFFFF' }}>
          <SchoolSpace
            schools={MOCK_SCHOOLS}
            selectedSchool={selectedSchool}
            onSelectedSchoolChange={(idx) =>
              setSelectedSchool(MOCK_SCHOOLS[idx])
            }
          />
        </PageLayout.SidebarLeft>
        <PageLayout.Content>
          <MessageFlashList
            messages={[
              {
                author: 'Platform Team',
                color: 'blue',
                contents: {
                  fr: "Chers étudiants, nous tenons à vous informer qu'un problème technique a affecté le site de Parcoursup, entraînant un report de toutes les inscriptions. Nous comprenons que cela puisse causer du stress et nous en sommes sincèrement désolés. Il est important de noter que notre établissement n'est pas responsable de cette situation, mais que cela provient directement de l'académie. Nous faisons de notre mieux pour vous fournir les informations les plus récentes et vous tiendrons informés dès que possible. Merci de votre compréhension. Nous vous proposons de suivre directement les informations de l’académie sur leurs site AcademieParcousup.fr",
                },
                endDate: '2050-03-27T23:59:59Z',
                id: '1',
                signature: 'Platform Announcements',
                startDate: '2026-01-20T00:00:00Z',
                title: 'Welcome to the new platform!',
              },
            ]}
          />

          <WelcomeWidget />

          {/** Service proposal widgets would go here, for example: */}
          <WidgetCard
            title="Vos services"
            style={{ marginBottom: '16px', marginTop: '16px' }}
          >
            <Grid>
              <Grid.Col sm="12" lg="6" className="d-flex flex-column gap-16">
                <LiensUtilesWidget items={MOCK_LIENS_UTILES} />
              </Grid.Col>
              <Grid.Col sm="12" lg="6" className="d-flex flex-column gap-16">
                <AvantagesWidget items={MOCK_AVANTAGES} />
              </Grid.Col>
            </Grid>
          </WidgetCard>

          {/** Service proposal widgets would go here, for example: */}
          <WidgetCard
            title="Vie scolaire"
            style={{ marginBottom: '16px', marginTop: '16px' }}
          >
            <Grid>
              <Grid.Col sm="12" lg="6" className="d-flex flex-column gap-16">
                <VieScolaireWidget kids={MOCK_VIE_SCOLAIRE} />
                <MesEmpruntsWidget items={MOCK_MES_EMPRUNTS} />
              </Grid.Col>
              <Grid.Col sm="12" lg="6" className="d-flex flex-column gap-16">
                <EmploiDuTempsWidget
                  date="Lundi 19 janvier"
                  entries={MOCK_EMPLOI_DU_TEMPS}
                  currentTimeIndex={0}
                />
              </Grid.Col>
            </Grid>
          </WidgetCard>
          <AppFooter />
        </PageLayout.Content>
      </PageLayout>
    </div>
  );
};

export default Root;
