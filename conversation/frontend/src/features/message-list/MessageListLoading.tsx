import { Avatar, Checkbox } from '@edifice.io/react';

export function MessageListLoading() {
  return (
    <>
      <div className="d-flex gap-16 align-items-center justify-content-between px-16 px-md-24 py-16 border-bottom">
        <Checkbox className="placeholder" disabled></Checkbox>
      </div>
      <div className="d-flex gap-24 px-16 py-12 mb-2 align-items-center ">
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
            <div className="d-block placeholder col-6"></div>
            <div className="placeholder col-10"></div>
          </div>
        </div>
      </div>
      <div className="d-flex gap-24 px-16 py-12 mb-2 align-items-center ">
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
            <div className="d-block placeholder col-5"></div>
            <div className="placeholder col-8"></div>
          </div>
        </div>
      </div>
      <div className="d-flex gap-24 px-16 py-12 mb-2 align-items-center ">
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
            <div className="d-block placeholder col-3"></div>
            <div className="placeholder col-9"></div>
          </div>
        </div>
      </div>
    </>
  );
}
