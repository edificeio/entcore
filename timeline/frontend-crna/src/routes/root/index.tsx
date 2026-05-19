import {
  LoadingScreen,
  PageLayout,
  useEdificeClient,
} from '@edifice.io/react';
import {
  LastInfosContainer,
  MessageFlashListContainer,
  SchoolSpace,
} from '@edifice.io/react/homepage';
import { useState } from 'react';
import backgroundImage from '~/assets/background.png';
import { MediacentreWidget, WidgetMasonry } from '~/components';
import { AppFooter } from '~/components/AppFooter';
import { AvantagesWidget } from '~/components/AvantagesWidget/AvantagesWidget';
import { BetaSwitchContainer } from '~/components/BetaSwitch/BetaSwitchContainer';
import { CarnetDeBordWidget } from '~/components/CarnetDeBordWidget';
import { WidgetErrorBoundary } from '~/components/ui/WidgetErrorBoundary';
import { WelcomeWidget } from '~/components/WelcomeWidget';

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
      <PageLayout.Header />
      <PageLayout.SidebarLeft className="d-grid align-content-start bg-white py-16 gap-16">
        <SchoolSpace
          schools={MOCK_SCHOOLS}
          selectedSchool={selectedSchool}
          onSelectedSchoolChange={(idx) => setSelectedSchool(MOCK_SCHOOLS[idx])}
        />
        <WidgetErrorBoundary>
          <LastInfosContainer />
        </WidgetErrorBoundary>
      </PageLayout.SidebarLeft>
      <PageLayout.Content className="d-grid align-content-start py-16 gap-16">
        <BetaSwitchContainer />
        {/* <MessageFlashList
          messages={[
            {
              author: 'Platform Team',
              color: 'blue',
              contents: {
                fr: "Chers étudiants, nous tenons à vous informer qu'un problème technique a affecté le site de Parcoursup, entraînant un report de toutes les inscriptions. Nous comprenons que cela puisse causer du stress et nous en sommes sincèrement désolés. Il est important de noter que notre établissement n'est pas responsable de cette situation, mais que cela provient directement de l'académie. Nous faisons de notre mieux pour vous fournir les informations les plus récentes et vous tiendrons informés dès que possible. Merci de votre compréhension. Nous vous proposons de suivre directement les informations de l'académie sur leurs site AcademieParcousup.fr",
              },
              endDate: '2050-03-27T23:59:59Z',
              id: '1',
              signature: 'Platform Announcements',
              startDate: '2026-01-20T00:00:00Z',
              title: 'Welcome to the new platform!',
            },
          ]}
        /> */}
        <MessageFlashListContainer />

        <WidgetErrorBoundary>
          <WelcomeWidget />
        </WidgetErrorBoundary>

        <WidgetMasonry>
          <MediacentreWidget />
          <WidgetErrorBoundary>
            <AvantagesWidget />
          </WidgetErrorBoundary>
          <WidgetErrorBoundary>
            <CarnetDeBordWidget />
          </WidgetErrorBoundary>
        </WidgetMasonry>
        {/* <WidgetCard
          title="Vos services"
          style={{ marginBottom: '16px', marginTop: '16px' }}
        >
          <Grid>
            <Grid.Col sm="12" lg="6" className="d-flex flex-column gap-16">
              <WidgetErrorBoundary>
                <LiensUtilesWidget />
              </WidgetErrorBoundary>
            </Grid.Col>
            <Grid.Col sm="12" lg="12" className="d-flex flex-column gap-16">
              <WidgetErrorBoundary>
                <AvantagesWidget />
              </WidgetErrorBoundary>
            </Grid.Col>
          </Grid>
        </WidgetCard>

        <WidgetCard
          title="Vie scolaire"
          style={{ marginBottom: '16px', marginTop: '16px' }}
        >
          <Grid>
            <Grid.Col sm="12" lg="6" className="d-flex flex-column gap-16">
              <WidgetErrorBoundary>
                <CarnetDeBordWidget />
              </WidgetErrorBoundary>
              <WidgetErrorBoundary>
                <MesEmpruntsWidget />
              </WidgetErrorBoundary>
            </Grid.Col>
            <Grid.Col sm="12" lg="6" className="d-flex flex-column gap-16">
              <WidgetErrorBoundary>
                <EmploiDuTempsWidget />
              </WidgetErrorBoundary>
            </Grid.Col>
          </Grid>
        </WidgetCard> */}
        <AppFooter />
      </PageLayout.Content>
    </PageLayout>
  );
};

export default Root;
