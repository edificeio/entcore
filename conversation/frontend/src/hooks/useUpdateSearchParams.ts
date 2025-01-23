// import { useEffect } from 'react';
// import { useSearchParams } from 'react-router-dom';
// import { useAppActions, useSearch, useUnread } from '~/store';

// export function useUpdateSearchParams() {
//   const [searchParams, setSearchParams] = useSearchParams();
//   const search = useSearch();
//   const unread = useUnread();
//   const { setSelectedMessageIds, setSearch, setUnread } = useAppActions();


//   useEffect(() => {
//     if (search !== undefined) {
//       if (search && search !== '') {
//         searchParams.set('search', search);
//       } else {
//         searchParams.delete('search');
//       }
//     }
//     if (unread !== undefined) {
//       if (unread) {
//         searchParams.set('unread', 'true');
//       } else {
//         searchParams.delete('unread');
//       }
//     }
//     setSelectedMessageIds([]);
//     setSearchParams(searchParams, { replace: true });

//     // eslint-disable-next-line react-hooks/exhaustive-deps
//   }, [search, searchParams, unread]);
// }
