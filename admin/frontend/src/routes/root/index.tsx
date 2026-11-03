import { LoadingScreen, useEdificeClient } from '@edifice.io/react';
import { ReportList } from '~/features/report-list/ReportList';
/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Root = () => {
  const { init } = useEdificeClient();

  if (!init) return <LoadingScreen position={false} />;

  return init ? (
    <div>
      <ReportList />
    </div>
  ) : null;
};

export default Root;
