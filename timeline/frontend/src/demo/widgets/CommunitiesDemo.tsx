import circus from '@images/backgrounds/circus.jpg';
import desert from '@images/backgrounds/desert.jpg';
import hills from '@images/backgrounds/hills.svg';
import ocean from '@images/backgrounds/ocean.jpg';
import panda from '@images/backgrounds/panda.jpg';
import sparkly from '@images/backgrounds/sparkly.jpg';
import { Communities, CommunityItemProps } from '@edifice.io/react/homepage';

/** Covers are backgrounds shipped by @edifice.io/bootstrap. */
const COMMUNITIES: CommunityItemProps[] = [
  { title: '5e B : vie de classe', communityImage: hills },
  { title: 'Club théâtre', communityImage: circus },
  { title: 'Voyage scolaire en Angleterre', communityImage: ocean },
  { title: 'Atelier robotique', communityImage: sparkly },
  { title: 'Projet « Biodiversité au collège »', communityImage: panda },
  { title: 'Association sportive : handball', communityImage: desert },
].map((community) => ({ ...community, onActionClick: () => {} }));

/** Demo "Mes communautés": a student's communities, who cannot create one. */
export function CommunitiesDemo() {
  return (
    <Communities
      communitiesList={COMMUNITIES}
      handleActionClick={() => {}}
      canCreateCommunity={false}
    />
  );
}
