import { categoryLabel } from './category-label';

describe('categoryLabel', () => {
  it('turns an enum value into a readable label', () => {
    expect(categoryLabel('MOROCCAN_HISTORY')).toBe('Moroccan History');
    expect(categoryLabel('EUROPEAN_HISTORY')).toBe('Europe History');
    expect(categoryLabel('SCIENTISTS_AND_INNOVATIONS')).toBe('Scientists & Innovations');
    expect(categoryLabel('NATURE_AND_ANIMALS')).toBe('Animals');
    expect(categoryLabel('FOOD_AND_CUISINE')).toBe('Food');
  });

  it('falls back to a readable version of unknown values', () => {
    expect(categoryLabel('NEW_CATEGORY')).toBe('New Category');
  });
});
