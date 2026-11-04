import TextField from '@mui/material/TextField';
import { useState } from 'react';

const MAX = 200;

export default function TextareaAvecCompteur() {
  const [value, setValue] = useState('');

  return (
    <TextField
      label="Description"
      multiline
      minRows={4}
      fullWidth
      value={value}
      onChange={(e) => setValue(e.target.value)}
      helperText={`${value.length} / ${MAX}`}
      error={value.length >= MAX}
      slotProps={{
        htmlInput: { maxLength: MAX }, // bloque la saisie au-delà
        formHelperText: { sx: { textAlign: 'right' } }, // compteur aligné à droite
      }}
    />
  );
}
