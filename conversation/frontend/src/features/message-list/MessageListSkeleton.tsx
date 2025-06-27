import {
  Avatar,
  ButtonSkeleton,
  Checkbox,
  TextSkeleton,
  useEdificeTheme,
} from '@edifice.io/react';

export function MessageListSkeleton({ withHeader = true }) {
  const { theme } = useEdificeTheme();

  return (
    <>
      {withHeader && (
        <>
          <div className="d-flex gap-16 align-items-center justify-content-between px-16 px-md-24 py-16 border-bottom">
            <ButtonSkeleton className="col-12 col-md-10"></ButtonSkeleton>
            {!theme?.is1d && (
              <ButtonSkeleton
                className="d-none d-md-block col-2"
                size="sm"
              ></ButtonSkeleton>
            )}
          </div>
          <div className="d-flex gap-16 align-items-center justify-content-between px-16 px-md-24 py-16 border-bottom">
            <Checkbox className="placeholder" disabled></Checkbox>
          </div>
        </>
      )}
      <div className="d-flex gap-24 px-16 py-12 mb-2 align-items-center">
        <div className="ps-md-8">
          <Checkbox className="placeholder" disabled></Checkbox>
        </div>
        <div className="d-flex align-items-center flex-fill gap-12 small">
          <Avatar
            alt=""
            size="sm"
            variant="circle"
            className="align-self-start mt-4 placeholder"
          />
          <div className="col-8">
            <TextSkeleton className="d-block col-6"></TextSkeleton>
            <TextSkeleton className="col-10"></TextSkeleton>
          </div>
        </div>
      </div>
      <div className="d-flex gap-24 px-16 py-12 mb-2 align-items-center">
        <div className="ps-md-8">
          <Checkbox className="placeholder" disabled></Checkbox>
        </div>
        <div className="d-flex align-items-center flex-fill gap-12 small">
          <Avatar
            alt=""
            size="sm"
            variant="circle"
            className="align-self-start mt-4 placeholder"
          />
          <div className="col-8">
            <TextSkeleton className="d-block col-5"></TextSkeleton>
            <TextSkeleton className="col-8"></TextSkeleton>
          </div>
        </div>
      </div>
      <div className="d-flex gap-24 px-16 py-12 mb-2 align-items-center">
        <div className="ps-md-8">
          <Checkbox className="placeholder" disabled></Checkbox>
        </div>
        <div className="d-flex align-items-center flex-fill gap-12 small">
          <Avatar
            alt=""
            size="sm"
            variant="circle"
            className="align-self-start mt-4 placeholder"
          />
          <div className="col-8">
            <TextSkeleton className="d-block col-3"></TextSkeleton>
            <TextSkeleton className="col-9"></TextSkeleton>
          </div>
        </div>
      </div>
    </>
  );
}
