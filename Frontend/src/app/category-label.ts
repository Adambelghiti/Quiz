const CATEGORY_LABELS: Record<string, string> = {
  MOROCCAN_HISTORY: 'Moroccan History',
  MOROCCAN_CULTURE: 'Moroccan Culture',
  ARABIC_CULTURE: 'Arabic Culture',
  ISLAMIC_CULTURE: 'Islamic Culture',
  EUROPEAN_HISTORY: 'Europe History',
  SCIENTISTS_AND_INNOVATIONS: 'Scientists & Innovations',
  GEOGRAPHY: 'Geography',
  SPORTS: 'Sports',
  MUSIC: 'Music',
  SCIENCE: 'Science',
  GENERAL_KNOWLEDGE: 'General Knowledge',
  TECHNOLOGY: 'Technology',
  LITERATURE: 'Literature',
  NATURE_AND_ANIMALS: 'Animals',
  FOOD_AND_CUISINE: 'Food',
  HEALTH_AND_BODY: 'Health & Body',
  ART_AND_ARCHITECTURE: 'Art & Architecture',
};

/** Human-readable label for a backend category enum value. */
export function categoryLabel(value: string): string {
  return (
    CATEGORY_LABELS[value] ??
    value
      .split('_')
      .filter(Boolean)
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ')
  );
}
