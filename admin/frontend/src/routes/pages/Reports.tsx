import Button from '@mui/material/Button';
import { ReportList } from '~/features/report-list/ReportList';
/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Component = () => {
  return (
    <div>
      <Button component="a" href="/">
        retour
      </Button>
      <ReportList />
    </div>
  );
};
