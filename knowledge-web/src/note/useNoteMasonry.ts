import { useLayoutEffect, useRef } from 'react';

/** Keep DOM/tab order equal to API order; place each card in the shortest column. */
export function useNoteMasonry(items: unknown) {
  const ref = useRef<HTMLDivElement>(null);
  useLayoutEffect(() => {
    const root = ref.current;
    if (!root) return;
    let frame = 0;
    const layout = () => {
      const width = root.clientWidth;
      if (!width) return;
      const gap = 20;
      const count = Math.max(1, Math.min(4, Math.floor((width + gap) / 280)));
      const cardWidth = (width - (count - 1) * gap) / count;
      const cards = Array.from(root.children) as HTMLElement[];
      cards.forEach(card => { card.style.width = `${cardWidth}px`; });
      const heights = cards.map(card => card.getBoundingClientRect().height);
      const columns = Array<number>(count).fill(0);
      cards.forEach((card, index) => {
        const column = columns.indexOf(Math.min(...columns));
        card.style.position = 'absolute';
        card.style.left = `${column * (cardWidth + gap)}px`;
        card.style.top = `${columns[column]}px`;
        columns[column] += heights[index] + gap;
      });
      root.style.height = `${Math.max(0, ...columns) - (cards.length ? gap : 0)}px`;
    };
    const observer = new ResizeObserver(() => {
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(layout);
    });
    observer.observe(root);
    Array.from(root.children).forEach(card => observer.observe(card));
    layout();
    return () => { observer.disconnect(); cancelAnimationFrame(frame); };
  }, [items]);
  return ref;
}
