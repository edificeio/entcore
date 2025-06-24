import { motion, AnimatePresence } from 'framer-motion';
import { useEffect, useState } from 'react';
import StarIcon from '~/assets/star.svg';

export function FavoriteStarIcon({ isFavorite }: { isFavorite: boolean }) {
  const [hasMounted, setHasMounted] = useState(false);

  useEffect(() => {
    setHasMounted(true);
  }, []);

  return (
    <AnimatePresence>
      {isFavorite && (
        <motion.img
          key="favorite-star"
          src={StarIcon}
          alt="star"
          className="favorite-star-icon"
          initial={hasMounted ? { scale: 0, rotate: 0, opacity: 0 } : false}
          animate={{
            scale: [0, 1.5, 1],
            rotate: 360,
            opacity: 1,
            transition: { duration: 0.8, ease: 'easeOut' },
          }}
          exit={{
            scale: 0,
            opacity: 0,
            transition: { duration: 0.3 },
          }}
        />
      )}
    </AnimatePresence>
  );
}