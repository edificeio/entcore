import { Layout, LoadingScreen, useEdificeClient } from '@edifice.io/react';

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

export const Root = () => {
  const { init } = useEdificeClient();

  if (!init) return <LoadingScreen position={false} />;

  return init ? <Layout>timeline</Layout> : null;
};

export default Root;
