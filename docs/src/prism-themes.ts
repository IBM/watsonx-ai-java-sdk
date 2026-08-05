import { themes as prismThemes } from 'prism-react-renderer';
import type { PrismTheme } from 'prism-react-renderer';

// ---- Light theme (based on github) ----
// Changes from base:
//   - string / attr-value / char  → green  #1a7f37
//   - class-name                  → purple #7c5cd8
export const lightTheme: PrismTheme = {
  ...prismThemes.github,
  styles: prismThemes.github.styles.map((entry) => {
    if (entry.types.includes('string') || entry.types.includes('attr-value')) {
      return { ...entry, style: { ...entry.style, color: '#1a7f37' } };
    }
    return entry;
  }).concat([
    { types: ['char'], style: { color: '#1a7f37' } },
    { types: ['class-name'], style: { color: '#7c5cd8' } },
  ]),
};

// ---- Dark theme (based on vsDark) ----
// Changes from base:
//   - string / attr-value / char / template-punctuation → green #57ab5a
//   - class-name                                         → purple #c5a6ff
export const darkTheme: PrismTheme = {
  ...prismThemes.vsDark,
  styles: prismThemes.vsDark.styles.map((entry) => {
    if (
      entry.types.includes('string') ||
      entry.types.includes('attr-value') ||
      entry.types.includes('char') ||
      entry.types.includes('template-punctuation')
    ) {
      return { ...entry, style: { ...entry.style, color: '#57ab5a' } };
    }
    if (entry.types.includes('class-name')) {
      return { ...entry, style: { ...entry.style, color: '#c5a6ff' } };
    }
    return entry;
  }),
};
