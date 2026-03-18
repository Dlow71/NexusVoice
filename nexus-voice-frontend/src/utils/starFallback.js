const encodeSvg = (svg) => `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;

const sparklesSvg = ({
  size = 96,
  background = '#e0f2fe',
  border = 'rgba(8,145,178,0.08)',
  color = '#0891b2',
  radius = 22,
  strokeWidth = 2.55,
}) => `
  <svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 96 96" fill="none">
    <rect x="0" y="0" width="96" height="96" rx="${radius}" fill="${background}" stroke="${border}" />
    <g stroke="${color}" stroke-width="${strokeWidth}" stroke-linecap="round" stroke-linejoin="round">
      <path d="M42.12 63.24a5.18 5.18 0 0 0-3.76-3.76l-15.2-3.92a1.32 1.32 0 0 1 0-2.56l15.2-3.92a5.18 5.18 0 0 0 3.76-3.76l3.92-15.2a1.32 1.32 0 0 1 2.56 0l3.92 15.2a5.18 5.18 0 0 0 3.76 3.76l15.2 3.92a1.32 1.32 0 0 1 0 2.56l-15.2 3.92a5.18 5.18 0 0 0-3.76 3.76l-3.92 15.2a1.32 1.32 0 0 1-2.56 0Z"/>
      <path d="M72 18v11"/>
      <path d="M77.5 23.5h-11"/>
      <path d="M21 69.5v5.5"/>
      <path d="M23.75 72.25h-5.5"/>
    </g>
  </svg>
`;

export const STAR_IMAGE_FALLBACK = encodeSvg(sparklesSvg({}));

export const replaceImageWithStarFallback = (target) => {
  if (!target || target.dataset.fallbackApplied === 'true') {
    return;
  }

  target.dataset.fallbackApplied = 'true';
  target.src = STAR_IMAGE_FALLBACK;
};

export const withStarFallback = (url) => url || STAR_IMAGE_FALLBACK;
