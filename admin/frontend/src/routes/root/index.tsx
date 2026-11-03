import { LoadingScreen, useEdificeClient } from '@edifice.io/react';
import Button from '@mui/material/Button';
import { useNavigate } from 'react-router-dom';
export const loader = async () => {
  return null;
};

export const Root = () => {
  const { init } = useEdificeClient();
  const navigate = useNavigate();

  if (!init) return <LoadingScreen position={false} />;

  return init ? (
    <div>
      <Button color="primary" onClick={() => navigate('/reports/fakeid')}>
        Go to reports
      </Button>
    </div>
  ) : null;
};

export default Root;
