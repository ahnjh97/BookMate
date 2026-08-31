(function initializeBookGenre(global) {
  const groups = {
    "소설": ["소설", "고전", "로맨스", "역사소설"],
    "판타지": ["판타지"],
    "SF": ["SF", "디스토피아"],
    "추리": ["추리", "스릴러"],
    "인문/사회": ["역사", "철학", "전기"],
    "자기계발": ["자기계발"],
    "과학/IT": ["과학", "IT"],
  };
  const genreToGroup = new Map();
  Object.entries(groups).forEach(([group, genres]) => {
    genres.forEach(genre => genreToGroup.set(genre, group));
  });

  global.BookMateGenre = {
    groups,
    groupOf(genre) {
      return genreToGroup.get(String(genre || "")) || "기타";
    },
  };
})(window);
