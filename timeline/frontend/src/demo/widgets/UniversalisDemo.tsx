import { School } from '@edifice.io/client';
import { Universalis } from '@edifice.io/react/homepage';

/** A single school with a UAI: the search is enabled, without school picker. */
const SCHOOL: School = {
  id: 'demo-school',
  name: 'Collège Jean Moulin',
  UAI: '0590001A',
  classes: ['5e B'],
  exports: [],
};

/** Demo "Universalis": opening Universalis or searching does not leave the demo. */
export function UniversalisDemo() {
  return (
    <Universalis
      handleActionClick={() => {}}
      onSearch={() => {}}
      selectedSchool={SCHOOL}
      onSelectedSchoolChange={() => {}}
      schools={[SCHOOL]}
    />
  );
}
